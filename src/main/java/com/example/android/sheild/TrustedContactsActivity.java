package com.example.android.sheild;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

/**
 * Shows a list of trusted contacts.
 * Click item => show phone # ; FAB => add new contact.
 */
public class TrustedContactsActivity extends AppCompatActivity {

    private final ArrayList<ContactModel> contactList = new ArrayList<>();
    private ContactAdapter adapter;
    private RecyclerView recycler;

    @Override
    protected void onCreate(@Nullable Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_trusted_contacts);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        /* Back button */
        ImageView back = findViewById(R.id.btnBack);
        back.setOnClickListener(v -> finish());

        /* Recycler setup */
        recycler = findViewById(R.id.recyclerContacts);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ContactAdapter(this ,contactList);
        recycler.setAdapter(adapter);


        /* FAB add-contact */
        FloatingActionButton fab = findViewById(R.id.fabAdd);
        fab.setOnClickListener(v -> showAddContactSheet());

    }

    /** Fetch trusted contacts from Firestore (Users/{uid}/contacts collection) */
    private void loadContacts() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseFirestore.getInstance()
                .collection("Users").document(uid).collection("contacts")
                .get()
                .addOnSuccessListener(snapshot -> {
                    contactList.clear();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        ContactModel c = doc.toObject(ContactModel.class);
                        contactList.add(c);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load contacts.", Toast.LENGTH_LONG).show());
    }
    private void showAddContactSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.add_contact, null);
        sheet.setContentView(view);

        EditText etName = view.findViewById(R.id.etContactName);
        EditText etPhone = view.findViewById(R.id.etContactPhone);
        Button btnSave = view.findViewById(R.id.btnSaveContact);

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();

            if (name.isEmpty()) {
                etName.setError("Name required");
                return;
            }
            if (phone.length() < 10 || phone.length() > 13) {
                etPhone.setError("Valid phone required");
                return;
            }

            saveContactToFirestore(name, phone);
            sheet.dismiss();
        });

        sheet.show();
    }

    private void saveContactToFirestore(String name, String phone) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Users").document(uid).collection("contacts")
                .add(new ContactModel(null, name, phone))
                .addOnSuccessListener(doc -> {
                    // Now update the Firestore doc with its generated ID
                    String docId = doc.getId();
                    doc.update("id", docId);

                    Toast.makeText(this, "Contact added!", Toast.LENGTH_SHORT).show();
                    loadContacts();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to save contact", Toast.LENGTH_LONG).show());
    }



    /* Reload list if user just added a contact */
    @Override protected void onResume() {
        super.onResume();
        loadContacts();
    }
}
