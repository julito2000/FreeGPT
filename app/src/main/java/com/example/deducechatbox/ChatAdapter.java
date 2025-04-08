package com.example.deducechatbox;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.deducechatbox.R;
import com.example.deducechatbox.MessageEntity;
import java.util.List;
import android.view.Gravity;
import android.widget.FrameLayout;
import androidx.constraintlayout.widget.ConstraintLayout;


public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private List<MessageEntity> messageList;


    public ChatAdapter(List<MessageEntity> messageList) {
        this.messageList = messageList;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        MessageEntity message = messageList.get(position);
        ConstraintLayout.LayoutParams params =
                (ConstraintLayout.LayoutParams) holder.messageText.getLayoutParams();
        holder.messageText.setText(message.getContent());
        holder.itemView.setAlpha(0f);
        holder.itemView.animate().alpha(1f).setDuration(300).start();

        // Set alignment based on sender
        if ("user".equals(message.getRole())) {
            holder.messageText.setBackgroundResource(R.drawable.user_bubble);
            params.startToStart = ConstraintLayout.LayoutParams.UNSET;
            params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
            //params.gravity = Gravity.END;
        } else {
            holder.messageText.setBackgroundResource(R.drawable.bot_bubble);
            params.endToEnd = ConstraintLayout.LayoutParams.UNSET;
            params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
            //params.gravity = Gravity.START;

        }
        //holder.messageText.setLayoutParams(params);
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;

        ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
        }
    }
}