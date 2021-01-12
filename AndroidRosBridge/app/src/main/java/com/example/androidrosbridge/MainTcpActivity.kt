package com.example.androidrosbridge

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import java.time.LocalDateTime
import java.time.LocalDateTime.*

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
class MainTcpActivity: AppCompatActivity(), SensorEventListener  {



    private var buttonScan: Button? = null
    private var ip_full_input: EditText? = null

    val rosBridgedataType: String? =  "\"sensor_msgs/Imu\"" //"\"geometry_msgs/PoseStamped\""
    val publish: String? = "publish"
    var advertise: String? = null
    var sequence_header_number: Int = 0
    var is_connected: Boolean? = false

    private var xyzList = arrayListOf<Float>(0.0F, 0.0F, 0.0F)
    private var coordinateScale = 1

    /* START basic logic from https://github.com/thezealousfool/IMU-Android */
    private lateinit var _sensorManager : SensorManager
    private lateinit var _acc : Sensor
    private lateinit var _gyro : Sensor
    private lateinit var _ori : Sensor // is a quaternion
    private var _acc_list = arrayListOf<Float>(0.0F, 0.0F, 0.0F)
    private var _gyro_list = arrayListOf<Float>(0.0F, 0.0F, 0.0F)
    private var _ori_list = arrayListOf<Float>(0.0F, 0.0F, 0.0F, 1.0F)

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }
    override fun onSensorChanged(event: SensorEvent?) {
        if(is_connected == true ) {
            when (event?.sensor?.type) {
                Sensor.TYPE_LINEAR_ACCELERATION -> {
                    _acc_list[0] = event?.values?.get(0)!!
                    _acc_list[1] = event?.values?.get(1)!!
                    _acc_list[2] = event?.values?.get(2)!!
                }
                Sensor.TYPE_GYROSCOPE -> {
                    _gyro_list[0] = event?.values?.get(0)!!
                    _gyro_list[1] = event?.values?.get(1)!!
                    _gyro_list[2] = event?.values?.get(2)!!
                }
                Sensor.TYPE_ROTATION_VECTOR -> {
                    _ori_list[0] = event?.values?.get(0)!!
                    _ori_list[1] = event?.values?.get(1)!!
                    _ori_list[2] = event?.values?.get(2)!!
                    _ori_list[3] = event?.values?.get(3)!!
                }
            }
            //processMessage()
        }
    }

    override fun onResume() {
        super.onResume()
        _sensorManager.registerListener(this, _acc, SensorManager.SENSOR_DELAY_NORMAL)
        _sensorManager.registerListener(this, _gyro, SensorManager.SENSOR_DELAY_NORMAL)
        _sensorManager.registerListener(this, _ori, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        _sensorManager.unregisterListener(this)
    }
    /* END basic logic from https://github.com/thezealousfool/IMU-Android */

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
                is_connected = true
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

        /* from https://github.com/thezealousfool/IMU-Android */
        _sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        _acc = _sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        _gyro = _sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        _ori = _sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

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

    @SuppressLint("NewApi")
    private fun processMessage(){
        var operation = "publish"
        if( advertise == null ){
            advertise = "advertise"
            operation = advertise as String
        }
        var secs: Int = (System.currentTimeMillis()*0.001).toInt()
        var nsecs: Int = now().nano
        var frame_id: String = "\"imu_link\""
        //val dataAsString = "\" %f, %f, %f \"".format(xyzList[0], xyzList[1], xyzList[2])
        //val dataAsMultiArray = "[ %f, %f, %f ]".format(xyzList[0], xyzList[1], xyzList[2])
        if( rosBridgedataType == "\"geometry_msgs/PoseStamped\"" ){
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
                        }}}""".format(sequence_header_number, secs, nsecs, frame_id,
                    pos_x, pos_y, pos_z,
                    ori_x, ori_y, ori_z, ori_w)
            mc!!.send(operation,rosBridgedataType, dataAsPoseStamped, true)
            Toast.makeText(this, operation + dataAsPoseStamped, Toast.LENGTH_LONG).show()
        } else if( rosBridgedataType == "\"sensor_msgs/Imu\"" )  {
            var lin_accel_x: Float = _acc_list[0]
            var lin_accel_y: Float = _acc_list[1]
            var lin_accel_z: Float = _acc_list[2]
            var ang_vel_x: Float = _gyro_list[0]
            var ang_vel_y: Float = _gyro_list[1]
            var ang_vel_z: Float = _gyro_list[2]
            var ori_x: Float = _ori_list[0]
            var ori_y: Float = _ori_list[1]
            var ori_z: Float = _ori_list[2]
            var ori_w: Float = _ori_list[3]
            var dataAsImu = """{"header":{ "seq": %d, 
                        "stamp":  {
                            "secs": %d,
                            "nsecs": %d
                        },
                        "frame_id": %s
                    }, "orientation": {
                            "x": %f,
                            "y": %f,
                            "z": %f,
                            "w": %f
                        }, "orientation_covariance": [ 0.0, 0.0, 0.0,   0.0, 0.0, 0.0,    0.0, 0.0, 0.0
                        ], "angular_velocity": {
                            "x": %f,
                            "y": %f,
                            "z": %f
                        }, "angular_velocity_covariance": [ 0.0, 0.0, 0.0,    0.0, 0.0, 0.0,    0.0, 0.0, 0.0
                        ], "linear_acceleration": {
                            "x": %f,
                            "y": %f,
                            "z": %f
                        }, "linear_acceleration_covariance": [ 0.0, 0.0, 0.0,    0.0, 0.0, 0.0,    0.0, 0.0, 0.0
                        ]}""".format(sequence_header_number, secs, nsecs, frame_id,
                    ori_x, ori_y, ori_z, ori_w,
                ang_vel_x, ang_vel_y, ang_vel_z,
                lin_accel_x, lin_accel_y, lin_accel_z)
            mc!!.send(operation,rosBridgedataType, dataAsImu, true)
        }
        sequence_header_number = sequence_header_number + 1
    }




}