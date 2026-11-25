package com.trainwise.app.domain
import com.trainwise.app.model.Workout

data class Analysis(val verdict:String,val good:List<String>,val fix:List<String>,val next:List<String>,val reaction:String)
object WorkoutAnalyzer {
    fun analyze(a:Workout):Analysis{
        val good=mutableListOf<String>(); val fix=mutableListOf<String>(); val next=mutableListOf<String>()
        var verdict="Оценка по доступным метрикам"
        if(a.type=="run"){
            a.hrDriftPct?.let { if(it<=3) good += "Пульсовой дрейф $it% — стабильная аэробная работа." else if(it>5) fix += "Пульсовой дрейф $it% высоковат: начать спокойнее и проверить восстановление." }
            a.rpe?.let { if(it<=4) good += "RPE $it/10 соответствует лёгкой базовой работе." else {fix += "RPE $it/10 выше цели базового блока."; verdict="Нагрузка выше цели"} }
            a.paceCv?.let { val p=(it*100).toInt(); if(it<=.08) good += "Темп был ровным: вариабельность около $p%." else if(it>.15) fix += "Темп был неровным ($p%). Easy лучше начинать спокойнее." }
            if(a.firstKmPaceSec!=null && a.avgPaceSec!=null && a.firstKmPaceSec<a.avgPaceSec-20) fix += "Первый километр был заметно быстрее среднего. Первые 10 минут лучше начинать медленнее."
            if((a.highHrPct?:0.0)>10 && a.highHrThreshold!=null) fix += "${a.highHrPct}% времени выше ${a.highHrThreshold} уд/мин. Для лёгкого бега желательно меньше времени выше Z2."
            a.trainingEffect?.let { if(it>=4){ fix += "Aerobic Training Effect $it — сильный стимул."; verdict="Значимая нагрузка" } else if(it>=2) good += "Aerobic Training Effect $it — аэробный стимул." }
            if(a.workoutKind=="long"){
                val share=if(a.weeklyRunKm!=null&&a.weeklyRunKm>0&&a.distanceKm!=null)(a.distanceKm/a.weeklyRunKm*100).toInt() else null
                if(share!=null&&share>55) fix += "Длительный составил около $share% недельного километража. Сам long не урезаем автоматически: наращиваем лёгкие километры среди недели."
                next += "Сохранять/растить длительный постепенно; длительный от 60 минут при достаточном восстановлении."
            } else next += "Следующую лёгкую пробежку начать спокойно первые 10 минут, RPE 2–3/10."
        } else if(a.type=="strength"){
            a.totalReps?.let { good += "$it записанных повторений." }
            if((a.maxHr?:0)>=155) fix += "Пульс доходил до ${a.maxHr}. Проверь достаточность отдыха между сетами."
            next += "Не увеличивать одновременно веса и число подходов. Сначала вернуть регулярность без отказа."
        } else {
            good += "Сессия учитывается как дополнительная нагрузка недели."
            next += "Учитывать её при планировании следующей тренировки."
        }
        val rb=a.recoveryBefore; val ra=a.recoveryNextDay
        val reaction=if(rb!=null&&ra!=null&&rb.hrv>0){
            val hp=((ra.hrv.toDouble()/rb.hrv-1)*100).toInt()
            "HRV $hp%, изменение пульса покоя ${ra.restingHr-rb.restingHr}, сна ${ra.sleepScore-rb.sleepScore}, Body Battery ${ra.bodyBatteryHigh-rb.bodyBatteryHigh}."
        } else "Нет данных следующего дня."
        if(good.isEmpty())good += "Недостаточно детальных метрик для уверенного положительного вывода."
        if(fix.isEmpty())fix += "Явных проблем по доступным метрикам нет; неполные данные ограничивают вывод."
        return Analysis(verdict,good,fix,next,reaction)
    }
}
