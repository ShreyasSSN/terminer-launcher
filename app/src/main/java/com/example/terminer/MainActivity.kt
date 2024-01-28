package com.example.terminer

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.terminer.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    lateinit var mainBinding: ActivityMainBinding
    private var installedApps = mutableListOf<String>()
    private var packageNames = mutableListOf<String>()
    private lateinit var adapter: AppDrawerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainBinding = ActivityMainBinding.inflate(layoutInflater)
        val view = mainBinding.root
        setContentView(view)

        mainBinding.recyclerViewApp.layoutManager =LinearLayoutManager(this@MainActivity)

        getInstalledApps()
        Log.d("Installed Apps in onCreate", "Total Apps: ${installedApps.size}")



        mainBinding.editTextSearchApp.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                val searchedText = s.toString()

                if (searchedText.isEmpty()){
                    adapter = AppDrawerAdapter(listOf(),this@MainActivity)
                    mainBinding.recyclerViewApp.adapter = adapter
                }else {
                    val suggestedApps =
                        installedApps.filter { it.contains(searchedText, ignoreCase = true) }
                    adapter = AppDrawerAdapter(suggestedApps, this@MainActivity)
                    mainBinding.recyclerViewApp.adapter = adapter
                }

            }

            override fun afterTextChanged(s: Editable?) {}

        })
    }

    private fun getInstalledApps(){
        installedApps.clear()
        val packageListInfo =packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
        for (packageInfo in packageListInfo){
            if (!isSystemPackage(packageInfo.applicationInfo)) {
                installedApps.add(packageInfo.applicationInfo.loadLabel(packageManager).toString())
                packageNames.add(packageInfo.packageName)
            }
        }
    }

    fun onAppClick(appName: String) {
        val index = installedApps.indexOf(appName)
        val appPackage = packageNames[index]
        val launchIntent = packageManager.getLaunchIntentForPackage(appPackage)
        if (launchIntent!=null){
            startActivity(launchIntent)
            mainBinding.editTextSearchApp.setText("")
        }else{
            Toast.makeText(applicationContext, "ERROR Opening App", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isSystemPackage(applicationInfo: ApplicationInfo): Boolean {
        return (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0) &&
                !isLauncherApp(applicationInfo.packageName)
    }

    private fun isLauncherApp(packageName: String): Boolean {
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfos = packageManager.queryIntentActivities(intent, 0)
        for (resolveInfo in resolveInfos) {
            if (resolveInfo.activityInfo.packageName == packageName) {
                return true
            }
        }
        return false
    }


}