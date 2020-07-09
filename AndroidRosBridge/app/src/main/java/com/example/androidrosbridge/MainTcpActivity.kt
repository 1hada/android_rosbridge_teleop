package com.example.androidrosbridge

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
/* Copyright (C) 2020 Jonathan Sanabria

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
class MainTcpActivity: AppCompatActivity() {


    private var buttonScan: Button? = null
    private var ip_full_input: EditText? = null

    val publish: String? = "publish"
    var advertise: String? = null

    private var xyzList = arrayListOf<Float>(0.0F, 0.0F, 0.0F)
    private var coordinateScale = 1

    private var mc: RosBridgeClient? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED)
        buttonScan = findViewById(R.id.scanBtn)
        ip_full_input = findViewById(R.id.ip_host_port_full)


        buttonScan!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(view: View) {

                var host_port_list: List<String> = processInputText( )
                var host:String = host_port_list[0] // "192.168.0.7:9090"
                var port:Int = host_port_list[1].toInt() // 9090
                mc = RosBridgeClient( host , port )
                mc!!.run()
            }
        })


        findViewById<Button>(R.id.X_add).setOnClickListener {
            countMe( 0 ,"+", R.id.X_val ) }
        findViewById<Button>(R.id.Y_add).setOnClickListener {
            countMe( 1 ,"+", R.id.Y_val ) }
        findViewById<Button>(R.id.Z_add).setOnClickListener {
            countMe( 2 ,"+", R.id.Z_val ) }
        findViewById<Button>(R.id.X_sub).setOnClickListener {
            countMe( 0 ,"-", R.id.X_val ) }
        findViewById<Button>(R.id.Y_sub).setOnClickListener {
            countMe( 1 ,"-", R.id.Y_val ) }
        findViewById<Button>(R.id.Z_sub).setOnClickListener {
            countMe( 2 ,"-" , R.id.Z_val )}

    }

    private fun processInputText(): List<String> {
        var inputString: String = ip_full_input?.text.toString()
        return inputString.split(":")

    }

    private fun countMe( listIdx: Int , axis_process: String , the_id : Int) {
        // Get the text view
        val showCountTextView = findViewById<TextView>(the_id)
        var numToChangeBy = 0

        if( axis_process == "+" ) {
            numToChangeBy = 1
        } else if( axis_process == "-" ) {
            numToChangeBy = -1
        }

        xyzList[listIdx] = xyzList[listIdx] + numToChangeBy*coordinateScale
        showCountTextView.text = xyzList[listIdx].toString()
        processMessage()
    }

    private fun processMessage(){
        var operation = "publish"
        if( advertise == null ){
            advertise = "advertise"
            operation = advertise as String
        }
        //val dataAsString = "\" %f, %f, %f \"".format(xyzList[0], xyzList[1], xyzList[2])
        //val dataAsMultiArray = "[ %f, %f, %f ]".format(xyzList[0], xyzList[1], xyzList[2])
        var seq: Int = 0
        var secs: Int = 0
        var nsecs: Int = 0
        var frame_id: String = "\"\""
        var pos_x: Float = xyzList[0]
        var pos_y: Float = xyzList[1]
        var pos_z: Float = xyzList[2]
        var ori_x: Float = 0F
        var ori_y: Float = 0F
        var ori_z: Float = 0F
        var ori_w: Float = 1F
        var dataAsPoseStamped = """{ "header": { "seq": %d, 
                        "stamp":  {
                            "secs": %d,
                            "nsecs": %d
                        },
                        "frame_id": %s
                    },
                    "pose": {
                        "position": {
                            "x": %f,
                            "y": %f,
                            "z": %f
                        },
                        "orientation": {
                            "x": %f,
                            "y": %f,
                            "z": %f,
                            "w": %f
                        }}}""".format( seq, secs, nsecs, frame_id,
            pos_x, pos_y, pos_z,
            ori_x, ori_y, ori_z, ori_w )
        mc!!.send( operation ,dataAsPoseStamped,true)
        Toast.makeText(this, operation + dataAsPoseStamped , Toast.LENGTH_LONG).show()

    }

}