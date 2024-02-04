package com.example.terminer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.terminer.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.Exception

class MainActivity : AppCompatActivity() {

    private lateinit var mainBinding: ActivityMainBinding
    private var installedApps = mutableListOf<DrawerItem>()
    private var contactsList = mutableListOf<DrawerItem>()
    private lateinit var drawerItemAdapter: DrawerItemAdapter
    private lateinit var timeUpdater: TimeUpdater
    private var userNumber = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainBinding = ActivityMainBinding.inflate(layoutInflater)
        val view = mainBinding.root
        setContentView(view)

        mainBinding.recyclerViewApp.layoutManager = LinearLayoutManager(this@MainActivity)
        getInstalledApps()

        drawerItemAdapter = DrawerItemAdapter(listOf(), this)
        mainBinding.recyclerViewApp.adapter = drawerItemAdapter


        mainBinding.editTextSearchApp.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                handleSearchTextChange(s)
            }
        })

        timeUpdater = TimeUpdater(
            onTimeUpdated = { updatedTime ->
                mainBinding.textViewTime.text = updatedTime
            },
            onDateUpdated = { updatedDate ->
                mainBinding.textViewDate.text = updatedDate
            })
    }

    private fun handleSearchTextChange(s: CharSequence?) {
        val searchedText = s.toString()
        searchForApps(searchedText)
    }

    private fun openContactList(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)!=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CONTACTS), 101)
        }else {
            getContactsDetails()
            drawerItemAdapter.updateDrawerItemList(contactsList)
        }
    }

    private fun searchForApps(searchedText : String){
        if (searchedText.isEmpty()) {
            drawerItemAdapter.updateDrawerItemList(emptyList())
        } else if ((searchedText.lowercase().trim() == "all apps") || (searchedText.lowercase() == "all")) {
            drawerItemAdapter.updateDrawerItemList(installedApps)
        } else if (searchedText.lowercase().startsWith("call ")){
            openContactList()
            searchContactList(searchedText.lowercase())
        } else if (searchedText.lowercase().startsWith("uninstall ")){
            searchAppsToUninstall(searchedText.lowercase())
        } else {
            val suggestedApps = installedApps.filter {
                it.name.contains(searchedText.trim(), ignoreCase = true)
            }
            drawerItemAdapter.updateDrawerItemList(suggestedApps)
        }
    }

    private fun searchAppsToUninstall(searchedApp : String){

        val unInstallApps = installedApps.map { it.copy(type = ItemType.UNINSTALL) }
        drawerItemAdapter.updateDrawerItemList(unInstallApps)
        val appName = searchedApp.removePrefix("uninstall ")
        val suggestedApps = unInstallApps.filter {
            it.name.contains(appName.trim(), ignoreCase = true)
        }

        drawerItemAdapter.updateDrawerItemList(suggestedApps)

    }

    private fun searchContactList(searchContact : String) {
        val contactName = searchContact.removePrefix("call ")
        if (contactName.all { it.isLetter() }){
            val suggestedContacts = contactsList.filter {
                it.name.contains(contactName.trim(), ignoreCase = true)
            }
            drawerItemAdapter.updateDrawerItemList(suggestedContacts)
        }else {
            userNumber = contactName
            val tempList = listOf(DrawerItem("-", "Number", userNumber, ItemType.CONTACT))
            drawerItemAdapter.updateDrawerItemList(tempList)
        }
    }


    private fun startCall(userNumber: String){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)!=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), 100)
        }
        else{
            val intent =Intent(Intent.ACTION_CALL)
            intent.data = Uri.parse("tel: $userNumber")
            startActivity(intent)
            mainBinding.editTextSearchApp.setText("")
        }
    }

    private fun getInstalledApps(){
        installedApps.clear()
        val packageListInfo =packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
        packageListInfo.sortBy {
            it.applicationInfo.loadLabel(packageManager).toString()
        }
        for (packageInfo in packageListInfo){
            if (!isSystemPackage(packageInfo.applicationInfo)) {
                val appId = packageInfo.packageName
                val appName = packageInfo.applicationInfo.loadLabel(packageManager).toString()

                installedApps.add(DrawerItem(id= appId, name = appName, type = ItemType.APP))
            }
        }
    }

    fun onAppClick(appPackageName: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(appPackageName)
        if (launchIntent!=null){
            startActivity(launchIntent)
            mainBinding.editTextSearchApp.setText("")
        }else{
            Toast.makeText(applicationContext, "ERROR Opening App", Toast.LENGTH_SHORT).show()
        }
    }

    fun onContactClick(contactName: String, contactNumber: String){
        mainBinding.editTextSearchApp.setText("call $contactName")
        startCall(contactNumber)
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

    @SuppressLint("Range")
    fun getContactsDetails(){
        var uniqueContactList = HashSet<DrawerItem>()
        val contentResolver = contentResolver
        var contactsCursor: Cursor? = null

        try {
            contactsCursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                null, null, null
            )
            if (contactsCursor!!.count > 0) {
                while (contactsCursor.moveToNext()) {
                    val rawContactId =
                        contactsCursor.getString(contactsCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NAME_RAW_CONTACT_ID))
                    val displayName =
                        contactsCursor.getString(contactsCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                    val phoneNumber =
                        contactsCursor.getString(contactsCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)).replace(" ", "")

                    val contact = DrawerItem(id = rawContactId, name = displayName, number = phoneNumber, type = ItemType.CONTACT)
                    uniqueContactList.add(contact)
                }
            }
        } finally {
            contactsCursor?.close()
        }

        contactsList = uniqueContactList.toMutableList()
        contactsList.sortBy {it.name}

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode ==101 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            openContactList()
        }else{
            Toast.makeText(this@MainActivity, "Permission Denied", Toast.LENGTH_SHORT)
                .show()
        }
        if(requestCode ==100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            val intent =Intent(Intent.ACTION_CALL)
            intent.data = Uri.parse("tel: $userNumber")
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timeUpdater.stopUpdates()
    }

    fun onUninstallClick(uninstallAppId: String) {
        mainBinding.editTextSearchApp.setText("")

        // Use a coroutine to perform the uninstallation in the background
        lifecycleScope.launch {
            try {
                uninstallAppInBackground(uninstallAppId)
            }catch (e: Exception){
                e.printStackTrace()
            }

        }
    }

    private suspend fun uninstallAppInBackground(uninstallAppId: String) {
        val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE).apply {
            data = Uri.parse("package:$uninstallAppId")
        }

        // Start the uninstallation on the background thread
        withContext(Dispatchers.IO) {
            try {
                startActivity(intent)
                onPause()
            } catch (e: Exception) {
                e.printStackTrace()
                // Handle exceptions as needed
            }
        }
        onResume()
        // Refresh the list of installed apps on the main thread
        withContext(Dispatchers.Main) {
            getInstalledApps()
            drawerItemAdapter.updateDrawerItemList(installedApps)
        }
    }
}
