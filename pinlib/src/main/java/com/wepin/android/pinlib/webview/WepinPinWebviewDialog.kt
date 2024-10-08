import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.Window
import android.webkit.WebView
import android.widget.FrameLayout
import com.wepin.android.pinlib.error.WepinError

class WepinPinWebviewDialog(context: Context, private val webView: WebView) : Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            // 타이틀바 제거
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            // 다이얼로그 및 웹뷰 배경을 투명하게 설정
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            webView.setBackgroundColor(Color.TRANSPARENT)
            webView.setLayerType(WebView.LAYER_TYPE_HARDWARE, null)

            // WebView 추가
            setContentView(FrameLayout(context).apply {
                addView(webView, FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                ))
            })
            setDialogSize()
        }catch (e: Exception){
            e.printStackTrace()
            throw WepinError.generalUnKnownEx(e.message)
        }
    }

    private fun setDialogSize() {
        // 이 시점에서는 window가 null이 아닐 것으로 기대
        val window = window
        if (window != null) {
            // 다이얼로그의 위치를 화면 아래쪽으로 정렬
            val layoutParams = window.attributes
            layoutParams.gravity = Gravity.BOTTOM
            window.attributes = layoutParams
        }
    }

    override fun dismiss() {
        super.dismiss()
        webView.destroy()
    }
}
