package com.example.android.sheild;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ContactViewHolder> {

    private final List<ContactModel> contacts;
    private final Context context;

    public ContactAdapter(Context context, List<ContactModel> list) {
        this.context = context;
        this.contacts = list;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_contact, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        ContactModel contact = contacts.get(position);
        holder.tvName.setText("👤 " + contact.getName());
        holder.tvPhone.setText("📞 " + contact.getPhone());
        holder.tvPhone.setVisibility(View.GONE);

        // Toggle phone visibility on tap
        holder.itemView.setOnClickListener(v -> {
            if (holder.tvPhone.getVisibility() == View.VISIBLE) {
                holder.tvPhone.setVisibility(View.GONE);
            } else {
                holder.tvPhone.setVisibility(View.VISIBLE);
            }
        });

        // Handle 3-dot menu
        holder.btnOptions.setOnClickListener(v -> {
            PopupMenu menu = new PopupMenu(context, v);
            menu.inflate(R.menu.menu_contact_options);
            menu.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.action_edit) {
                    showEditDialog(contact, holder.getAdapterPosition());
                    return true;
                } else if (id == R.id.action_delete) {
                    deleteContact(holder.getAdapterPosition());
                    return true;
                }
                return false;
            });
            menu.show();
        });
    }

    // Show in-place edit dialog
    private void showEditDialog(ContactModel contact, int position) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_contact, null);
        EditText etName = dialogView.findViewById(R.id.etEditName);
        EditText etNumber = dialogView.findViewById(R.id.etEditNumber);

        etName.setText(contact.getName());
        etNumber.setText(contact.getPhone());

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Edit Contact")
                .setView(dialogView)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String newName = etName.getText().toString().trim();
                String newNumber = etNumber.getText().toString().trim();

                if (newName.isEmpty() || newNumber.isEmpty()) {
                    Toast.makeText(context, "Fields cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }

                String uid = FirebaseAuth.getInstance().getUid();
                if (uid == null) return;

                contact.setName(newName);
                contact.setPhone(newNumber);

                FirebaseFirestore.getInstance()
                        .collection("Users").document(uid)
                        .collection("contacts").document(contact.getId())
                        .set(contact)
                        .addOnSuccessListener(unused -> {
                            contacts.set(position, contact);
                            notifyItemChanged(position);
                            Toast.makeText(context, "Contact updated", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(context, "Failed to update: " + e.getMessage(), Toast.LENGTH_LONG).show());
            });
        });

        dialog.show();
    }




    // Remove contact from Firestore + list
    private void deleteContact(int position) {
        ContactModel contact = contacts.get(position);
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseFirestore.getInstance()
                .collection("Users").document(uid)
                .collection("contacts").document(contact.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    contacts.remove(position);
                    notifyItemRemoved(position);
                    Toast.makeText(context, "Contact deleted", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(context, "Delete failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }


    @Override
    public int getItemCount() {
        return contacts.size();
    }

    static class ContactViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPhone;
        ImageView btnOptions;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvContactName);
            tvPhone = itemView.findViewById(R.id.tvContactNumber);
            btnOptions = itemView.findViewById(R.id.btnOptions);
        }
    }
}
