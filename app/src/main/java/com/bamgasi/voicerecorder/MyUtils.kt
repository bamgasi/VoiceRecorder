package com.bamgasi.voicerecorder

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.TypedValue
import android.widget.Toast

class MyUtils {
    companion object {
        /**
         * google playstore = PLAYSTORE
         * onestore = ONESTORE
         */
        private const val TARGET_STORE = "PLAYSTORE"
        private const val ONESTORE_ID = "00751820"

        fun kakaoShare() {
            val context = MyApplication.instance as Context
            val targetUrl = if (TARGET_STORE == "ONESTORE") {
                "https://onesto.re/$ONESTORE_ID"
            }else{
                "https://play.google.com/store/apps/details?id=${context.packageName}"
            }

            try{
                val intent = Intent(Intent.ACTION_SEND)
                intent.type = "text/plain"
                intent.setPackage("com.kakao.talk")
                intent.putExtra(Intent.EXTRA_TEXT, targetUrl)
                context.startActivity(Intent.createChooser(intent, "공유하기").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "공유 중 알 수 없는 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        fun shareApp() {
            val context = MyApplication.instance as Context
            val targetUrl = if (TARGET_STORE == "ONESTORE") {
                "https://onesto.re/$ONESTORE_ID"
            }else{
                "https://play.google.com/store/apps/details?id=${context.packageName}"
            }

            try{
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "text/plain"
                shareIntent.putExtra(Intent.EXTRA_TEXT, targetUrl)
                context.startActivity(Intent.createChooser(shareIntent, "공유하기").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "공유 중 알 수 없는 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        fun goToStore() {
            val context = MyApplication.instance as Context
            val targetUrl = if (TARGET_STORE == "ONESTORE") {
                "onestore://common/product/$ONESTORE_ID"
            }else{
                "https://play.google.com/store/apps/details?id=${context.packageName}"
            }

            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "알 수 없는 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        fun introMyApp() {
            val context = MyApplication.instance as Context
            val targetUrl = if (TARGET_STORE == "ONESTORE") {
                "https://m.onestore.co.kr/mobilepoc/web/apps/appsDetail/more/sellerOtherProduct.omp?sellerKey=SE202009171652576260053029&prodId=${ONESTORE_ID}"
            }else{
                "https://play.google.com/store/apps/developer?id=Coco+Lab"
            }

            try{
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "알 수 없는 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        fun Int.toDp(context: Context):Int = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,this.toFloat(),context.resources.displayMetrics
        ).toInt()

        fun getVersion(): String {
            val context = MyApplication.instance as Context
            val pkgInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            return pkgInfo.versionName
        }

        fun convertAppVersion(version: String): Int {
            val splitArray: List<String> = version.split(".")
            val major: Int = Integer.parseInt(splitArray[0]) * 100
            val mid: Int = Integer.parseInt(splitArray[1]) * 10
            val miner: Int = Integer.parseInt(splitArray[2]) * 1
            return major + mid + miner
        }
    }

}