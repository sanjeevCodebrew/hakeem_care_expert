package com.consultantvendor.ui.dashboard.salla

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.consultantvendor.R
import com.consultantvendor.data.models.responses.SallaCheckoutOption
import com.consultantvendor.data.models.responses.SallaProduct
import com.consultantvendor.databinding.ItemSallaProductBinding

class SallaProductsAdapter(
    private val items: ArrayList<SallaProduct>,
    private val onSelectionChanged: (selectedCount: Int) -> Unit,
    private val onProductTapped: (product: SallaProduct, position: Int) -> Unit
) : RecyclerView.Adapter<SallaProductsAdapter.ViewHolder>() {

    // productId → (chosen options, chosen quantity)
    private val selectedData = mutableMapOf<Long, Pair<List<SallaCheckoutOption>, Int>>()

    fun setProductSelected(productId: Long, options: List<SallaCheckoutOption>, quantity: Int, position: Int) {
        selectedData[productId] = options to quantity
        notifyItemChanged(position)
        onSelectionChanged(selectedData.size)
    }

    fun deselectProduct(productId: Long, position: Int) {
        selectedData.remove(productId)
        notifyItemChanged(position)
        onSelectionChanged(selectedData.size)
    }

    // Returns (product, options, quantity)
    fun getSelectedProducts(): List<Triple<SallaProduct, List<SallaCheckoutOption>, Int>> =
        items.filter { it.id != null && selectedData.containsKey(it.id) }
            .map { Triple(it, selectedData[it.id]!!.first, selectedData[it.id]!!.second) }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSallaProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(private val binding: ItemSallaProductBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(product: SallaProduct, position: Int) {
            Glide.with(binding.root.context)
                .load(product.thumbnail ?: product.main_image)
                .placeholder(R.drawable.image_placeholder)
                .error(R.drawable.image_placeholder)
                .into(binding.ivProduct)

            binding.tvName.text = product.name ?: ""

            val amount = product.price?.amount
            val currency = product.price?.currency
            binding.tvPrice.text = if (amount != null && currency != null) "${amount.toInt()} $currency" else ""

            binding.tvStatus.text = product.status ?: ""
            binding.tvStatus.setTextColor(
                binding.root.context.getColor(
                    if (product.status == "sale") R.color.colorPrimary else R.color.textAge
                )
            )

            val isSelected = product.id != null && selectedData.containsKey(product.id)
            binding.cbSelect.isChecked = isSelected

            val opts = product.options?.filter { !it.values.isNullOrEmpty() }
            if (!opts.isNullOrEmpty()) {
                binding.tvOptions.isVisible = true
                val primaryColor = ContextCompat.getColor(binding.root.context, R.color.colorPrimary)
                val textColor = ContextCompat.getColor(binding.root.context, R.color.textColor_50)
                val ssb = SpannableStringBuilder()

                if (isSelected && product.id != null) {
                    val chosen = selectedData[product.id]?.first
                    opts.forEachIndexed { i, opt ->
                        if (i > 0) ssb.append("\n")
                        val label = "${opt.name ?: ""}: "
                        ssb.append(label)
                        ssb.setSpan(StyleSpan(Typeface.BOLD), ssb.length - label.length, ssb.length, 0)
                        ssb.setSpan(ForegroundColorSpan(textColor), ssb.length - label.length, ssb.length, 0)
                        val chosenVal = chosen?.find { it.id == opt.id }
                        val valueName = opt.values?.find { it.id?.toString() == chosenVal?.value }?.name ?: ""
                        val start = ssb.length
                        ssb.append(valueName)
                        ssb.setSpan(ForegroundColorSpan(primaryColor), start, ssb.length, 0)
                    }
                } else {
                    opts.forEachIndexed { i, opt ->
                        if (i > 0) ssb.append("\n")
                        val label = "${opt.name ?: ""}: "
                        ssb.append(label)
                        ssb.setSpan(StyleSpan(Typeface.BOLD), ssb.length - label.length, ssb.length, 0)
                        ssb.setSpan(ForegroundColorSpan(textColor), ssb.length - label.length, ssb.length, 0)
                        val values = opt.values!!
                        val preview = values.take(3).joinToString("  ·  ") { it.name ?: "" }
                        val suffix = if (values.size > 3) "  +${values.size - 3}" else ""
                        val start = ssb.length
                        ssb.append(preview + suffix)
                        ssb.setSpan(ForegroundColorSpan(textColor), start, ssb.length, 0)
                    }
                }
                binding.tvOptions.text = ssb
            } else {
                binding.tvOptions.isVisible = false
            }

            binding.root.setOnClickListener {
                val id = product.id ?: return@setOnClickListener
                if (selectedData.containsKey(id)) {
                    deselectProduct(id, position)
                } else {
                    onProductTapped(product, position)
                }
            }
        }
    }
}
