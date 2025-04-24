package com.consultantvendor.ui.loginSignUp.login

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.consultantvendor.R
import com.consultantvendor.data.models.responses.LoggedInUser
import com.consultantvendor.databinding.ItemUserBinding
import com.consultantvendor.utils.loadImage


class LoginUserAdapter(
    private val users: List<LoggedInUser>,
    private val onSelect: (position:Int) -> Unit
) : RecyclerView.Adapter<LoginUserAdapter.OptionViewHolder>() {

    private val limitedUsers = users.takeLast(3).reversed()

    inner class OptionViewHolder(val binding: ItemUserBinding) :
        RecyclerView.ViewHolder(binding.root){
            init {
                binding.clMain.setOnClickListener {
                    onSelect(absoluteAdapterPosition)
                }
            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OptionViewHolder {
        val binding = ItemUserBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OptionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OptionViewHolder, position: Int) {
        val item = users[position]

        holder.binding.tvUsername.text = item.username
        if (item.profileImageUrl.isNotEmpty()) {
            loadImage(holder.binding.ivPic, item.profileImageUrl, R.drawable.ic_profile_placeholder)
        } else {
            holder.binding.ivPic.setImageResource(R.drawable.image_placeholder)
        }

    }
    override fun getItemCount(): Int = limitedUsers.size

}


