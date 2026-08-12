package com.consultantvendor.ui.loginSignUp.login

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.consultantvendor.R
import com.consultantvendor.data.models.responses.UserSession
import com.consultantvendor.data.repos.UserRepository
import com.consultantvendor.databinding.ItemUserBinding
import com.consultantvendor.utils.gone
import com.consultantvendor.utils.loadImage
import com.consultantvendor.utils.visible


class LoginUserAdapter(
    private val users: List<UserSession>,
    private val userRepository: UserRepository,
    private val onSelect: (position: Int) -> Unit
) : RecyclerView.Adapter<LoginUserAdapter.OptionViewHolder>() {

//    private val limitedUsers = users.takeLast(4).reversed()

    inner class OptionViewHolder(val binding: ItemUserBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
               binding.clMain.setOnClickListener {
                   users[absoluteAdapterPosition].isSelect =true
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
        holder.binding.tvUsername.text = item.moh
        if (item.profileImageUrl.isNotEmpty()) {
            loadImage(holder.binding.ivPic, item.profileImageUrl, R.drawable.ic_profile_placeholder)
        } else {
            holder.binding.ivPic.setImageResource(R.drawable.image_placeholder)
        }

        if (userRepository.getUser()?.moh_number==item.moh){
            holder.binding.ivTick.visible()
        }
        else
        {
            holder.binding.ivTick.gone()
        }

    }

    override fun getItemCount(): Int = users.size


}


