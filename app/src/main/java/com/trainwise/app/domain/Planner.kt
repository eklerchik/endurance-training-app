package com.trainwise.app.domain
import com.trainwise.app.model.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlin.math.max
import kotlin.math.roundToInt

object Planner {
    fun recoveryScore(s: AppState): Int? {
        val hrv = s.todayHrv ?: return null
        val baseline = s.baselineHrv?.takeIf { it > 0 } ?: return null
        val resting = s.todayRestingHr ?: return null
        val restingBase = s.baselineRestingHr ?: return null
        val sleep = s.todaySleepHours ?: return null
        val sleepBase = s.baselineSleepHours ?: return null
        val battery = s.todayBodyBattery ?: return null
        val c = s.checkIn
        var score = 72.0
        score += ((hrv.toDouble()/baseline-1)*110).coerceIn(-18.0,14.0)
        score += ((restingBase-resting)*2.5).coerceIn(-15.0,10.0)
        score += ((sleep-sleepBase)*7).coerceIn(-14.0,10.0)
        score += ((battery-60)*.22).coerceIn(-10.0,10.0)
        score -= max(0,c.fatigue-2)*5
        score -= max(0,c.legs-2)*4
        score -= if(c.pain>=3) 22 else c.pain*3
        return score.roundToInt().coerceIn(15,96)
    }
    private fun durationByReadiness(score:Int,min:Int,max:Int) = when {
        score<55 -> min
        score<75 -> (((min+max)/2.0)/5).roundToInt()*5
        else -> max
    }
    private fun scaled(v:Int,pct:Int)=((v*pct/100.0)/5).roundToInt()*5
    fun plan(s:AppState,today:LocalDate=LocalDate.now()):List<PlannedSession>{
        val measured=recoveryScore(s)
        val score=measured ?: 54
        val b=s.trainingBlock
        val monday=today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val safety=s.checkIn.pain>=3 || score<40
        return (0..6).map { i ->
            val d=monday.plusDays(i.toLong())
            fun easy(lo:Int,hi:Int,exercises:Boolean=false):PlannedSession {
                val run=scaled(durationByReadiness(score,lo,hi),b.weeklyVolumePct)
                return if(run>=b.minRunMinutes) PlannedSession(d,if(exercises)"Бег + упражнения" else "Бег",if(exercises)"Z2 + стопа / голеностоп / колено" else "Z2",run,"Z2 · RPE 2–4",if(exercises)"Спокойный бег, затем 12–15 мин упражнений." else "Спокойный бег во 2 зоне.",if(exercises)"$run мин + 12–15 мин" else "$run мин")
                else if(exercises) PlannedSession(d,"Упражнения","Стопа / голеностоп / колено",0,"Очень легко","Бег ниже заданного минимума.","12–15 мин")
                else PlannedSession(d,"Восстановление","Без бега",0,"Отдых","Бег ниже заданного минимума.","отдых")
            }
            var session=when(d.dayOfWeek){
                DayOfWeek.MONDAY -> PlannedSession(d,"Отдых","Полный выходной",0,"Без спорта","Понедельник всегда полный выходной.","выходной")
                DayOfWeek.TUESDAY -> easy(45,60,true)
                DayOfWeek.WEDNESDAY -> easy(30,40)
                DayOfWeek.THURSDAY -> PlannedSession(d,"Силовая","Умеренная силовая",0,"Без отказа","Не вытесняем силовую лёгким бегом.","умеренная силовая")
                DayOfWeek.FRIDAY -> easy(45,60)
                DayOfWeek.SATURDAY -> if(b.bikeGear) PlannedSession(d,"Вело + силовая","Z2 bike + небольшая силовая",0,"Z2 · без отказа","Лёгкая велотренировка и небольшая силовая.","вело Z2 + силовая") else PlannedSession(d,"Восстановление","Отдых / мобилити",0,"Очень легко","Пока нет велоодежды.","отдых / мобилити")
                DayOfWeek.SUNDAY -> if(b.sundayMode=="trail" && measured!=null && score>=55) PlannedSession(d,"Трейл","Спокойный трейл",0,"RPE 3–4","Спокойный трейл 10–15 км.","10–15 км",12.5) else {
                    val base=b.longRunMinutes.coerceIn(60,b.longRunTargetMinutes.coerceAtLeast(60))
                    val mins=if(measured==null) 60 else if(score<55) base-10 else base
                    if(mins<60) PlannedSession(d,"Восстановление","Без длительного",0,"Отдых","Восстановление не позволяет поставить длительный от 60 мин.","отдых")
                    else PlannedSession(d,"Бег","Длительный лёгкий бег",mins,"Z2 · RPE 2–4","Постепенное увеличение только при стабильном восстановлении.","$mins мин")
                }
            }
            if(safety && d.dayOfWeek!=DayOfWeek.MONDAY) session=PlannedSession(d,"Восстановление","Отдых / очень легко",0,"Отдых","При боли 3+/10 или очень низком recovery нагрузку отменяем.","отдых")
            session
        }
    }
    fun weeklyKmRange(s:AppState,plan:List<PlannedSession>):Triple<Double,Double,Double>{
        var min=0.0; var max=0.0
        for(p in plan){
            if(p.distanceKm!=null){min+=p.distanceKm;max+=p.distanceKm}
            else if(p.type.startsWith("Бег")&&p.minutes>0){
                min += p.minutes*60.0/s.trainingBlock.z2PaceMaxSec
                max += p.minutes*60.0/s.trainingBlock.z2PaceMinSec
            }
        }
        fun r(v:Double)=kotlin.math.round(v*10)/10
        return Triple(r(min),r(max),r((min+max)/2))
    }
}
