package com.unciv.ui.worldscreen.optionstable

import com.unciv.logic.GameSaver
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

class DropBox(){

    /**
     * @param dropboxApiArg If true, then the data will be sent via a Dropbox-Api-Arg header and not via the post body
     */
    fun dropboxApi(url:String, data:String="",contentType:String="",dropboxApiArg:String=""):String {

        with(URL(url).openConnection() as HttpURLConnection) {
            requestMethod = "POST"  // optional default is GET

            setRequestProperty("Authorization", "Bearer LTdBbopPUQ0AAAAAAAACxh4_Qd1eVMM7IBK3ULV3BgxzWZDMfhmgFbuUNF_rXQWb")

            if (dropboxApiArg != "") setRequestProperty("Dropbox-API-Arg", dropboxApiArg)
            if (contentType != "") setRequestProperty("Content-Type", contentType)

            doOutput = true

            try {
                if (data != "") {
                    val postData: ByteArray = data.toByteArray(StandardCharsets.UTF_8)
                    val outputStream = DataOutputStream(outputStream)
                    outputStream.write(postData)
                    outputStream.flush()
                }

                val reader = BufferedReader(InputStreamReader(inputStream))
                val output = reader.readText()

                println(output)
                return output
            } catch (ex: Exception) {
                println(ex.message)
                val reader = BufferedReader(InputStreamReader(errorStream))
                println(reader.readText())
                return "Error!"
            }
        }
    }

    fun getFolderList(folder:String):FolderList{
        val response = dropboxApi("https://api.dropboxapi.com/2/files/list_folder",
                "{\"path\":\"$folder\"}","application/json")
        return GameSaver().json().fromJson(FolderList::class.java,response)
    }

    fun downloadFile(fileName:String):String{
        val response = dropboxApi("https://content.dropboxapi.com/2/files/download",
                contentType = "text/plain",dropboxApiArg = "{\"path\":\"$fileName\"}")
        return response
    }

    fun uploadFile(fileName: String, data: String){
        val response = dropboxApi("https://content.dropboxapi.com/2/files/upload",
                data,"application/octet-stream","{\"path\":\"$fileName\"}")
    }


    class FolderList{
        var entries = ArrayList<FolderListEntry>()
    }

    class FolderListEntry{
        var name=""
        var path_display=""
    }

}