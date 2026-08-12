package com.consultantvendor.ui.dashboard.zid

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.consultantvendor.R
import com.consultantvendor.data.models.responses.ZidProduct
import com.consultantvendor.databinding.ItemZidProductBinding

class ZidProductsAdapter(
    private val items: ArrayList<ZidProduct>,
    private val onSelectionChanged: (selectedCount: Int) -> Unit,
    private val onProductTapped: (product: ZidProduct, position: Int) -> Unit
) : RecyclerView.Adapter<ZidProductsAdapter.ViewHolder>() {

    // All items from API (used for local search filtering)
    private val allItems = ArrayList<ZidProduct>()

    fun submitAllItems(list: List<ZidProduct>) {
        val copy = ArrayList(list) // snapshot before clearing — items & list are same reference
        allItems.clear()
        allItems.addAll(copy)
        items.clear()
        items.addAll(copy)
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        items.clear()
        if (query.isBlank()) {
            items.addAll(allItems)
        } else {
            val q = query.trim().lowercase()
            items.addAll(allItems.filter { it.displayName.lowercase().contains(q) })
        }
        notifyDataSetChanged()
    }

    // sku → chosen quantity
    private val selectedData = mutableMapOf<String, Int>()

    fun setProductSelected(sku: String, quantity: Int, position: Int) {
        selectedData[sku] = quantity
        notifyItemChanged(position)
        onSelectionChanged(selectedData.size)
    }

    fun deselectProduct(sku: String, position: Int) {
        selectedData.remove(sku)
        notifyItemChanged(position)
        onSelectionChanged(selectedData.size)
    }

    // Returns (product, quantity)
    fun getSelectedProducts(): List<Pair<ZidProduct, Int>> =
        items.filter { it.sku != null && selectedData.containsKey(it.sku) }
            .map { it to (selectedData[it.sku] ?: 1) }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemZidProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(private val binding: ItemZidProductBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(product: ZidProduct, position: Int) {
            Glide.with(binding.root.context)
                .load(product.images?.firstOrNull()?.image?.thumbnail)
                .placeholder(R.drawable.image_placeholder)
                .error(R.drawable.image_placeholder)
                .into(binding.ivProduct)

            binding.tvName.text = product.displayName

            binding.tvPrice.text = product.formatted_price
                ?: product.price?.let { "$it ${product.currency ?: ""}" } ?: ""

            binding.tvStatus.text = binding.root.context.getString(
                if (product.inStock) R.string.in_stock else R.string.out_of_stock
            )
            binding.tvStatus.setTextColor(
                binding.root.context.getColor(
                    if (product.inStock) R.color.colorPrimary else R.color.textAge
                )
            )

            val isSelected = product.sku != null && selectedData.containsKey(product.sku)
            binding.cbSelect.isChecked = isSelected

            binding.tvOptions.isVisible = false

            binding.root.setOnClickListener {
                val sku = product.sku ?: return@setOnClickListener
                if (selectedData.containsKey(sku)) {
                    deselectProduct(sku, position)
                } else {
                    onProductTapped(product, position)
                }
            }
        }
    }
}
