package com.example.terminer

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.terminer.databinding.SearchedAppListBinding


class AppDrawerAdapter(private var searchedAppList: List<String>, private val clickListener: MainActivity)
    : RecyclerView.Adapter<AppDrawerAdapter.AppViewHolder>() {

    inner class AppViewHolder(val adapterBinding: SearchedAppListBinding)
        : RecyclerView.ViewHolder(adapterBinding.root)


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding =SearchedAppListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AppViewHolder(binding)
    }

    override fun getItemCount(): Int {

        return searchedAppList.size

    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.adapterBinding.textViewAppName.text = searchedAppList[position]
        holder.adapterBinding.linearLayout.setOnClickListener {
            Log.d("Clicked App", "Clicked App is ${searchedAppList[position]}")
            clickListener.onAppClick(searchedAppList[position])
        }
    }
}