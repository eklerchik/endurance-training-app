package com.trainwise.app.data
import android.content.Context
import com.trainwise.app.model.*
import org.json.JSONArray
import org.json.JSONObject

class AppRepository(context: Context) {
    private val prefs = context.getSharedPreferences("trainwise-native", Context.MODE_PRIVATE)
    private val key = "state-v14"
    fun load(): AppState {
        val raw = prefs.getString(key, null) ?: return AppState()
        return runCatching { fromJson(JSONObject(raw)) }.getOrElse { AppState() }
    }
    fun save(s: AppState) {
        val o = JSONObject(prefs.getString(key, null)?.let { runCatching { JSONObject(it) }.getOrNull() }?.toString() ?: "{}")
        o.put("schemaVersion", s.schemaVersion)
        o.put("baselineHrv", s.baselineHrv ?: JSONObject.NULL)
        o.put("baselineRestingHr", s.baselineRestingHr ?: JSONObject.NULL)
        o.put("baselineSleepHours", s.baselineSleepHours ?: JSONObject.NULL)
        o.put("todayHrv", s.todayHrv ?: JSONObject.NULL)
        o.put("todayRestingHr", s.todayRestingHr ?: JSONObject.NULL)
        o.put("todaySleepHours", s.todaySleepHours ?: JSONObject.NULL)
        o.put("todayBodyBattery", s.todayBodyBattery ?: JSONObject.NULL)
        o.put("todayStress", s.todayStress ?: JSONObject.NULL)
        o.put("checkIn", JSONObject().apply {
            put("fatigue", s.checkIn.fatigue); put("legs", s.checkIn.legs); put("pain", s.checkIn.pain)
            put("motivation", s.checkIn.motivation); put("note", s.checkIn.note)
        })
        o.put("trainingBlock", JSONObject().apply {
            with(s.trainingBlock) {
                put("longRunMinutes", longRunMinutes); put("longRunTargetMinutes", longRunTargetMinutes)
                put("sundayMode", sundayMode); put("bikeGear", bikeGear); put("z2PaceMinSec", z2PaceMinSec)
                put("z2PaceMaxSec", z2PaceMaxSec); put("weeklyVolumePct", weeklyVolumePct); put("minRunMinutes", minRunMinutes)
            }
        })
        o.put("goal", JSONObject().apply {
            put("name", s.goal.name); put("date", s.goal.date); put("priority", s.goal.priority); put("note", s.goal.note)
        })
        o.put("races", JSONArray().apply { s.races.forEach { r -> put(JSONObject().apply {
            put("id",r.id); put("name",r.name); put("date",r.date); put("distance",r.distance)
            put("type",r.type); put("priority",r.priority); put("target",r.target)
        }) } })
        prefs.edit().putString(key, o.toString()).apply()
    }
    private fun fromJson(o: JSONObject): AppState {
        val d = AppState()
        val c = o.optJSONObject("checkIn")
        val b = o.optJSONObject("trainingBlock")
        val g = o.optJSONObject("goal")
        val racesJson = o.optJSONArray("races")
        val races = if (racesJson != null) buildList {
            for (i in 0 until racesJson.length()) {
                val r = racesJson.getJSONObject(i)
                add(Race(r.optString("id"), r.optString("name"), r.optString("date"), r.optString("distance"), r.optString("type","road"), r.optString("priority","C"), r.optString("target")))
            }
        } else d.races
        fun int(k: String): Int? = if (o.isNull(k)) null else o.optInt(k)
        fun double(k: String): Double? = if (o.isNull(k)) null else o.optDouble(k).takeIf { it.isFinite() }
        return d.copy(
            baselineHrv = int("baselineHrv"), baselineRestingHr = int("baselineRestingHr"), baselineSleepHours = double("baselineSleepHours"),
            todayHrv = int("todayHrv"), todayRestingHr = int("todayRestingHr"), todaySleepHours = double("todaySleepHours"),
            todayBodyBattery = int("todayBodyBattery"), todayStress = int("todayStress"),
            checkIn = CheckIn(c?.optInt("fatigue",2) ?: 2,c?.optInt("legs",2) ?: 2,c?.optInt("pain",0) ?: 0,c?.optInt("motivation",4) ?: 4,c?.optString("note","") ?: ""),
            trainingBlock = TrainingBlock(
                (b?.optInt("longRunMinutes",75) ?: 75).coerceIn(60,110),
                (b?.optInt("longRunTargetMinutes",110) ?: 110).coerceIn(60,180),
                b?.optString("sundayMode","long") ?: "long", b?.optBoolean("bikeGear",false) ?: false,
                (b?.optInt("z2PaceMinSec",380) ?: 380).coerceAtLeast(60),
                (b?.optInt("z2PaceMaxSec",400) ?: 400).coerceAtLeast(60),
                (b?.optInt("weeklyVolumePct",50) ?: 50).coerceIn(40,100),
                (b?.optInt("minRunMinutes",30) ?: 30).coerceIn(15,90)
            ),
            goal = Goal(g?.optString("name",d.goal.name) ?: d.goal.name,g?.optString("date",d.goal.date) ?: d.goal.date,g?.optString("priority","A") ?: "A",g?.optString("note",d.goal.note) ?: d.goal.note),
            races = races
        )
    }
}
