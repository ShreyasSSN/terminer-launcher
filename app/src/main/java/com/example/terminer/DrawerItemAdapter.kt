package com.example.terminer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.terminer.databinding.SearchedAppListBinding

class DrawerItemAdapter(
    private var drawerItem : List<DrawerItem>,
    private val clickListener : MainActivity
): RecyclerView.Adapter<DrawerItemAdapter.DrawerViewHolder>() {

    inner class DrawerViewHolder(val adapterBinding: SearchedAppListBinding) :
            RecyclerView.ViewHolder(adapterBinding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DrawerViewHolder {
        val binding =  SearchedAppListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DrawerViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return  drawerItem.size
    }

    override fun onBindViewHolder(holder: DrawerViewHolder, position: Int) {
        val drawerItem = drawerItem[position]
        if (drawerItem.number == null) {
            holder.adapterBinding.textViewAppName.text = drawerItem.name
        }else{
            holder.adapterBinding.textViewAppName.text = drawerItem.name + " :: " + drawerItem.number
        }
        holder.adapterBinding.linearLayout.setOnClickListener {
            when(drawerItem.type){
                ItemType.APP -> clickListener.onAppClick(drawerItem.id)
                ItemType.UNINSTALL -> clickListener.onUninstallClick(drawerItem.id)
                ItemType.CONTACT -> clickListener.onContactClick(drawerItem.name, drawerItem.number!!)
            }
        }
    }

    fun updateDrawerItemList(newList : List<DrawerItem>){
        drawerItem = newList
        notifyDataSetChanged()
    }
}