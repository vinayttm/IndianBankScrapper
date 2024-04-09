package com.app.IndianBankScrapperr.Services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.app.IndianBankScrapperr.ApiManager
import com.app.IndianBankScrapperr.Config
import com.app.IndianBankScrapperr.MainActivity
import com.app.IndianBankScrapperr.Utils.AES
import com.app.IndianBankScrapperr.Utils.AccessibilityUtil
import com.app.IndianBankScrapperr.Utils.AutoRunner
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.lang.reflect.Field
import java.util.Arrays


class RecorderService : AccessibilityService() {
    private val ticker = AutoRunner(this::initialStage)
    private var appNotOpenCounter = 0
    private val apiManager = ApiManager()
    private val au = AccessibilityUtil()
    private var isLogin = false
    private var miniStatementOnce = false;
    private var aes = AES()


    override fun onServiceConnected() {
        super.onServiceConnected()
        ticker.startRunning()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {

    }

    override fun onInterrupt() {
    }


    private fun initialStage() {
        Log.d("initialStage", "initialStage  Event")
        printAllFlags().let { Log.d("Flags", it) }
        ticker.startReAgain()
        if (!MainActivity().isAccessibilityServiceEnabled(this, this.javaClass)) {
            return;
        }
        val rootNode: AccessibilityNodeInfo? = au.getTopMostParentNode(rootInActiveWindow)
        if (rootNode != null) {
            if (au.findNodeByPackageName(rootNode, Config.packageName) == null) {
                if (appNotOpenCounter > 2) {
                    Log.d("App Status", "Not Found")
                    relaunchApp()
                    try {
                        Thread.sleep(4000)
                    } catch (e: InterruptedException) {
                        throw RuntimeException(e)
                    }
                    appNotOpenCounter = 0
                    isLogin = false
                    isSelectedOnes = false;
                    isSelectedAccount = false;
                    isViewButton = false
                    return
                }
                appNotOpenCounter++
            } else {
                checkForSessionExpiry()
                au.listAllTextsInActiveWindow(au.getTopMostParentNode(rootInActiveWindow))
                apiManager.checkUpiStatus { isActive ->
                    if (isActive) {
                        enterPin()
                        mPassbook()
                        if (au.listAllTextsInActiveWindow(au.getTopMostParentNode(rootInActiveWindow))
                                .contains("View")
                        ) {
                            pleaseSelect()
                            dialogBox()
                            viewButton()
                            Thread.sleep(1500)
                            scrollToGetMore()
                        }

                    } else {
                        closeAndOpenApp()

                    }
                }
            }
            rootNode.recycle()
        }
    }

    private var scrollCounter = 0

    private fun scrollToGetMore() {
        if (!au.listAllTextsInActiveWindow(au.getTopMostParentNode(rootInActiveWindow))
                .contains("No Records Found")
        ) {
            val scrollLayout =
                au.findNodeByClassName(rootInActiveWindow, "android.widget.ScrollView")
            if (scrollLayout != null && scrollCounter < 6) {
                Thread.sleep(2000)
                val scrollBounds = Rect()
                scrollLayout.getBoundsInScreen(scrollBounds)
                val startX = scrollBounds.centerX()
                val startY = scrollBounds.centerY()
                val scrollDistance = 250
                val endY = startY - scrollDistance
                val path = Path()
                path.moveTo(startX.toFloat(), startY.toFloat())
                path.lineTo(startX.toFloat(), endY.toFloat())
                val gestureBuilder = GestureDescription.Builder()
                gestureBuilder.addStroke(StrokeDescription(path, 0, 100))
                dispatchGesture(gestureBuilder.build(), null, null)
                scrollCounter++
                if (scrollCounter == 6) {
                    isViewButton = false
                }
                readTransaction(scrollCounter)
                scrollLayout.recycle()
            }
        }

    }


    private fun closeAndOpenApp() {
        performGlobalAction(GLOBAL_ACTION_BACK)
        isLogin = false
        isSelectedOnes = false;
        isSelectedAccount = false;
        isViewButton = false
        scrollCounter = 0;
//        val intent = packageManager.getLaunchIntentForPackage(Config.packageName)
//        if (intent != null) {
//            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//            startActivity(intent)
//            isLogin = false
//        } else {
//            Log.e("AccessibilityService", "App not found: " + Config.packageName)
//        }
    }


    private fun enterPin() {
        val loginPin = Config.loginPin;
        if (loginPin.isEmpty()) {
            return
        }
        val loginWithPIN =
            au.findNodeByText(
                au.getTopMostParentNode(rootInActiveWindow),
                "Enter 4 Digit Login PIN",
                false,
                false
            )
        loginWithPIN?.apply {
            if (isLogin) return
            val mPinTextField =
                au.findNodeByClassName(rootInActiveWindow, "android.widget.EditText")
            mPinTextField?.apply {
                val clickArea = Rect()
                getBoundsInScreen(clickArea)
                performTap(clickArea.centerX().toFloat(), clickArea.centerY().toFloat(), 180)
                Thread.sleep(500)
                ticker.startReAgain()
                val mainList =
                    au.listAllTextsInActiveWindow(au.getTopMostParentNode(rootInActiveWindow))
                if (mainList.contains("PCI-DSS")) {
                    val aNoIndex = mainList.indexOf("PCI-DSS")
                    val separatedList =
                        mainList.subList(aNoIndex, mainList.size).toMutableList()
                    println("separatedList $separatedList")
                    if (separatedList.contains("PCI-DSS")) {
                        for (c in loginPin.toCharArray()) {
                            val numberNode =
                                au.findNodeByText(rootInActiveWindow, c.toString(), true, false)
                            Thread.sleep(1000)
                            numberNode?.apply {
                                println(" number ${numberNode.text}")
                                val clickArea = Rect()
                                getBoundsInScreen(clickArea)
                                performTap(
                                    clickArea.centerX().toFloat(),
                                    clickArea.centerY().toFloat(),
                                    180
                                )
                            }
                        }
                    }
                }
                isLogin = true
            }
        }
    }


    private var isSelectedOnes = false;
    private var isSelectedAccount = false;
    private var isViewButton = false

    private fun pleaseSelect() {
        if (!isSelectedOnes) {
            performTap(384.toFloat(), 131.toFloat(), 950)
            isSelectedOnes = true
            isSelectedAccount = false
        }
    }


    private fun dialogBox() {
        if (!isSelectedAccount) {
            Thread.sleep(3000)
            performTap(870.toFloat(), 473.toFloat(), 950)
            isSelectedAccount = true
        }
    }

    private fun viewButton() {
        if (!isViewButton) {
            Thread.sleep(1500)
            performTap(1143.toFloat(), 145.toFloat(), 950)
            isViewButton = true
            scrollCounter = 0;
        }
    }


    private fun mPassbook() {
        val mPassbook = au.findNodeByText(rootInActiveWindow, "m-Passbook", true, false)
        mPassbook?.parent?.apply {
            performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
    }


    private fun filterList(): MutableList<String> {
        val mainList = au.listAllTextsInActiveWindow(au.getTopMostParentNode(rootInActiveWindow))
        val mutableList = mutableListOf<String>()
        if (mainList.contains("Balance")) {
            val unfilteredList = mainList.filter { it.isNotEmpty() }
            val aNoIndex = unfilteredList.indexOf("Balance")
            val separatedList = unfilteredList.subList(aNoIndex, unfilteredList.size)
                .filter { it != "Balance" }
                .toMutableList()
            println("modifiedStrings $separatedList")
            mutableList.addAll(separatedList)
        }
        return mutableList
    }


    private fun readTransaction(index: Int) {
        println("scroll index $index")
        ticker.startReAgain()
        val output = JSONArray()
        val mainList = au.listAllTextsInActiveWindow(au.getTopMostParentNode(rootInActiveWindow))
        try {
            if (mainList.contains("Balance")) {
                val filterList = filterList();
                for (i in filterList.indices step 5) {
                    val date = filterList[i]
                    val description = filterList[i + 1]
                    val withdraw = filterList[i + 2]
                    val deposit = filterList[i + 3]
                    var amount = ""
                    if (withdraw.contains("-")) {
                        amount = deposit.replace("₹", "").replace(" ", "").trim()
                    }
                    if (deposit.contains("-")) {
                        amount = "-${withdraw}".replace("₹", "").replace(" ", "").trim()
                    }
                    val accountBalance = filterList[i + 4].replace("₹", "").replace(" ", "").trim()
                    val entry = JSONObject()
                    try {
                        entry.put("Amount", amount.replace(",", "").trim())
                        entry.put("RefNumber", extractUTRFromDesc(description))
                        entry.put("Description", extractUTRFromDesc(description))
                        entry.put(
                            "AccountBalance",
                            accountBalance.replace(",", "").replace("CR", "").trim()
                        )
                        entry.put("CreatedDate", date)
                        entry.put("BankName", Config.bankName + Config.bankLoginId)
                        entry.put("BankLoginId", Config.bankLoginId)
                        entry.put("UPIId", getUPIId(description))
                        output.put(entry)
                    } catch (e: JSONException) {
                        throw java.lang.RuntimeException(e)
                    }

                }
                Log.d("Final Json Output", output.toString());
                Log.d("Total length", output.length().toString());
                if (output.length() > 0) {
                    val result = JSONObject()
                    try {
                        result.put("Result", aes.encrypt(output.toString()))
                        apiManager.saveBankTransaction(result.toString());
                        ticker.startReAgain()
                    } catch (e: JSONException) {
                        throw java.lang.RuntimeException(e)
                    }
                }

            }
        } catch (ignored: Exception) {

        }
    }


    private val queryUPIStatus = Runnable {
        val intent = packageManager.getLaunchIntentForPackage(Config.packageName)
        intent?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(this)
        }
    }
    private val inActive = Runnable {
        Toast.makeText(this, "BandhanBankScrapper inactive", Toast.LENGTH_LONG).show();
    }

    private fun relaunchApp() {
        apiManager.queryUPIStatus(queryUPIStatus, inActive)
    }


    private fun checkForSessionExpiry() {
        val node1 = au.findNodeByText(
            rootInActiveWindow,
            "Session Timeout. Please Login again to continue.",
            false,
            false
        )
        val node2 = au.findNodeByText(
            rootInActiveWindow,
            "Session Timeout. Please Login again to continue.",
            false,
            false
        )
        val node3 = au.findNodeByText(
            rootInActiveWindow,
            "Do you want to Logout?.",
            false,
            false
        )
        val node4 = au.findNodeByText(
            rootInActiveWindow,
            "Session Timeout Alert",
            false,
            false
        )
        //Session Timeout Alert
        //Do you want to Logout?
        //Session Timeout. Please Login again to continue.
        node1?.apply {
            val okButton = au.findNodeByText(rootInActiveWindow, "OK", false, false)
            okButton?.apply {
                performAction(AccessibilityNodeInfo.ACTION_CLICK)
                recycle()
                isLogin = false
                isSelectedOnes = false;
                isSelectedAccount = false;
                isViewButton = false
                ticker.startReAgain()

            }
        }
        node2?.apply {
            val okButton = au.findNodeByText(rootInActiveWindow, "Ok", false, false)
            okButton?.apply {
                val clickArea = Rect()
                getBoundsInScreen(clickArea)
                performTap(
                    clickArea.centerX().toFloat(),
                    clickArea.centerY().toFloat(),
                    180
                )
                recycle()
                isLogin = false
                isSelectedOnes = false;
                isSelectedAccount = false;
                isViewButton = false
                ticker.startReAgain()

            }
        }
        val okButton = au.findNodeByText(rootInActiveWindow, "YES", true, false)
        okButton?.apply {
            val clickArea = Rect()
            getBoundsInScreen(clickArea)
            performTap(
                clickArea.centerX().toFloat(),
                clickArea.centerY().toFloat(),
                90
            )
            isLogin = false
            isSelectedOnes = false;
            isSelectedAccount = false;
            isViewButton = false
            ticker.startReAgain()

        }
        node4?.apply {
            val okButton = au.findNodeByText(rootInActiveWindow, "Keep me logged in", false, false)
            okButton?.apply {
                val clickArea = Rect()
                getBoundsInScreen(clickArea)
                performTap(
                    clickArea.centerX().toFloat(),
                    clickArea.centerY().toFloat(),
                    180
                )
                recycle()
                ticker.startReAgain()

            }
        }
        val maybeLater = au.findNodeByText(rootInActiveWindow, "Maybe Later", false, false)
        maybeLater?.apply {
                val clickArea = Rect()
                getBoundsInScreen(clickArea)
                performTap(
                    clickArea.centerX().toFloat(),
                    clickArea.centerY().toFloat(),
                    180
                )
            val intent = packageManager.getLaunchIntentForPackage(packageName.toString())
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                isLogin = false
            } else {
                Log.e("AccessibilityService", "App not found: " + packageName.toString())
            }
            ticker.startReAgain()

    }
        //Maybe Later
    }


    private fun performTap(x: Float, y: Float, duration: Long) {
        Log.d("Accessibility", "Tapping $x and $y")
        val p = Path()
        p.moveTo(x, y)
        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(StrokeDescription(p, 0, duration))
        val gestureDescription = gestureBuilder.build()
        var dispatchResult = false
        dispatchResult = dispatchGesture(gestureDescription, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription) {
                super.onCompleted(gestureDescription)
            }
        }, null)
        Log.d("Dispatch Result", dispatchResult.toString())
    }

    private fun getUPIId(description: String): String {
        if (!description.contains("@")) return ""
        val split: Array<String?> =
            description.split("/".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
        var value: String? = null
        value = Arrays.stream(split).filter { x: String? ->
            x!!.contains(
                "@"
            )
        }.findFirst().orElse(null)
        return value ?: ""

    }

    private fun extractUTRFromDesc(description: String): String? {
        return try {
            val split: Array<String?> =
                description.split("/".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
            var value: String? = null
            value = Arrays.stream(split).filter { x: String? -> x!!.length == 12 }
                .findFirst().orElse(null)
            if (value != null) {
                "$value $description"
            } else description
        } catch (e: Exception) {
            description
        }

    }


    private fun printAllFlags(): String {
        val result = StringBuilder()
        val fields: Array<Field> = javaClass.declaredFields
        for (field in fields) {
            field.isAccessible = true
            val fieldName: String = field.name
            try {
                val value: Any? = field.get(this)
                result.append(fieldName).append(": ").append(value).append("\n")
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            }
        }
        return result.toString()
    }

}
