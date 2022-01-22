package com.example.fast2calculator

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import androidx.room.Room
import com.example.fast2calculator.model.History
import java.lang.NumberFormatException

class MainActivity : AppCompatActivity() {

    private val expressionTextView: TextView by lazy {
        findViewById(R.id.expressionTextView)
    }

    private val resultTextView : TextView by lazy {
        findViewById(R.id.resultTextView)
    }

    private val historyLayout: View by lazy {
        findViewById(R.id.historyLayout)
    }

    private val historyLinearLayout : LinearLayout by lazy {
        findViewById(R.id.historyLinearLayout)
    }

    lateinit var db: AppDataBase

    private var isOperator = false
    private var hasOperator = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = Room.databaseBuilder(
            applicationContext,
            AppDataBase::class.java,
            "historyDB"
        ).build()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun buttonClicked(v: View) {
        when(v.id) {
            R.id.btn_zero -> numberButtonClicked("0")
            R.id.btn_one -> numberButtonClicked("1")
            R.id.btn_two -> numberButtonClicked("2")
            R.id.btn_three -> numberButtonClicked("3")
            R.id.btn_four -> numberButtonClicked("4")
            R.id.btn_five -> numberButtonClicked("5")
            R.id.btn_six -> numberButtonClicked("6")
            R.id.btn_seven -> numberButtonClicked("7")
            R.id.btn_eight -> numberButtonClicked("8")
            R.id.btn_nine -> numberButtonClicked("9")
            R.id.btn_plus -> operatourButtonClicked("+")
            R.id.btn_minus -> operatourButtonClicked("-")
            R.id.btn_multi -> operatourButtonClicked("*")
            R.id.btn_divider -> operatourButtonClicked("/")
            R.id.btn_modulo -> operatourButtonClicked("%")
        }
    }

    private fun numberButtonClicked(number: String) {

        if (isOperator) {
            expressionTextView.append(" ")
        }
        isOperator = false
        val expressionText = expressionTextView.text.split(" ")
        //숫자가 비어있지 않고 마지막글자가 15글자 이상이면
        if(expressionText.isNotEmpty() && expressionText.last().length >= 15) {
            Toast.makeText(this,"15자리 까지만 사용할 수 있습니다.",Toast.LENGTH_SHORT).show()
            return
        } else if(expressionText.last().isEmpty() && number == "0") {
            Toast.makeText(this,"0은 제일 앞에 올 수 없습니다.",Toast.LENGTH_SHORT).show()
        }
        expressionTextView.append(number)

        // resultTextView에 실시간으로 계산 결과를 넣어야 하는 기능
        resultTextView.text = calculateExpression()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun operatourButtonClicked(operator: String) {
        if (expressionTextView.text.isEmpty()) {
            return
        }
        when {
            isOperator -> {
                val text = expressionTextView.text.toString()
                expressionTextView.text = text.dropLast(1) + operator
            }
            hasOperator -> {
                Toast.makeText(this,"연산자는 한 번만 사용할 수 있습니다..",Toast.LENGTH_SHORT).show()
            }
            else -> {
                expressionTextView.append(" ${operator}")
            }
        }
        val ssb = SpannableStringBuilder(expressionTextView.text)
        ssb.setSpan(
            ForegroundColorSpan(getColor(R.color.green)),
            expressionTextView.text.length -1,
            expressionTextView.text.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        expressionTextView.text = ssb

        isOperator = true
        hasOperator = true
    }
    fun clearButtonClicked(v: View) {
        expressionTextView.text = ""
        resultTextView.text = ""
        isOperator = false
        hasOperator = false
    }

    fun historyButtonClicked(v: View) {
        historyLayout.isVisible = true
        historyLinearLayout.removeAllViews()
        // 디비에서 기록 자겨오기
        Thread(Runnable {
            db.historyDao().getAll().reversed().forEach{
                runOnUiThread {
                    val historyView = LayoutInflater.from(this).inflate(R.layout.history_row,null,false)
                    historyView.findViewById<TextView>(R.id.expressionTextView).text = it.expression
                    historyView.findViewById<TextView>(R.id.resultTextView).text = "= ${it.result}"

                    historyLinearLayout.addView(historyView)
                }
            }
        }).start()

    }

    fun closeHistoryButtonClicked(v: View) {
        // 히스토리 레이아웃 안보이게 바꾸기
        historyLayout.isVisible = false
    }
    fun historyClearBUttonClicked(v: View) {
        // 디비에서 모든 기록 삭제
        historyLinearLayout.removeAllViews()
        // 뷰에서 모든 기록 삭제
        Thread(Runnable {
            db.historyDao().deleteAll()
        }).start()


    }



    fun resultButtonClicked(v: View) {
        val expressionTexts = expressionTextView.text.split(" ")

        if(expressionTextView.text.isEmpty() || expressionTexts.size ==1) {
            return
        }

        if (expressionTexts.size != 3 && hasOperator) {
            Toast.makeText(this,"아직 완성되지 않은 수식입니다.",Toast.LENGTH_SHORT).show()
            return
        }
        if (!expressionTexts[0].isNumber() || !expressionTexts[2].isNumber()) {
            Toast.makeText(this,"오류가 발생했습니다.",Toast.LENGTH_SHORT).show()
            return
        }
        val expressionText = expressionTextView.text.toString()
        val resultText = calculateExpression()

        //DB에 넣어주는 부분
        Thread(Runnable {
            db.historyDao().insertHistory(History(null,expressionText,resultText))
        }).start()

        resultTextView.text = ""
        expressionTextView.text = resultText

        isOperator = false
        hasOperator = false
    }

    //결과를 가져와 resultTextView에 넣어줌
    private fun calculateExpression(): String {
        val expressionTexts = expressionTextView.text.split(" ")

        if (hasOperator.not() || expressionTexts.size != 3) {
            return ""
        } else if(!expressionTexts[0].isNumber() || !expressionTexts[2].isNumber()) {
            return ""
        }
        val exp1 = expressionTexts[0].toBigInteger()
        val exp2 = expressionTexts[2].toBigInteger()
        val op = expressionTexts[1]

        return when(op) {
            "+" -> (exp1 + exp2).toString()
            "-" -> (exp1 - exp2).toString()
            "*" -> (exp1 * exp2).toString()
            "/" -> (exp1 / exp2).toString()
            "%" -> (exp1 % exp2).toString()
            else -> ""
        }
    }
}

fun String.isNumber(): Boolean {
    return try {
        this.toBigInteger()
        return true
    } catch (e: NumberFormatException) {
        false
    }
}