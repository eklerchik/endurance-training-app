package com.trainwise.app.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trainwise.app.data.AppRepository
import com.trainwise.app.domain.Planner
import com.trainwise.app.domain.WorkoutAnalyzer
import com.trainwise.app.model.*
import java.time.LocalDate
import java.util.UUID
import kotlin.math.roundToInt

private enum class Screen(val label:String){ TODAY("Сегодня"), WEEK("Неделя"), ANALYSIS("Анализ"), CHECKIN("Check-in"), GOALS("Цели"), RACES("Забеги"), DATA("Данные") }
private class Controller(private val repo:AppRepository){
    var state by mutableStateOf(repo.load()); private set
    fun update(v:AppState){state=v;repo.save(v)}
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun TrainwiseApp(){
    val context=LocalContext.current
    val c=remember{Controller(AppRepository(context))}
    var screen by remember{mutableStateOf(Screen.TODAY)}
    var menu by remember{mutableStateOf(false)}
    Scaffold(
        topBar={ TopAppBar(title={Column{Text("Trainwise",fontWeight=FontWeight.Bold);Text(screen.label,style=MaterialTheme.typography.labelSmall)}},actions={
            Box { TextButton(onClick={menu=true}){Text("Меню")}
                DropdownMenu(expanded=menu,onDismissRequest={menu=false}) {
                    Screen.entries.forEach { s -> DropdownMenuItem(text={Text(s.label)},onClick={screen=s;menu=false}) }
                }
            }
        }) },
        bottomBar={ NavigationBar { listOf(Screen.TODAY,Screen.WEEK,Screen.ANALYSIS,Screen.CHECKIN).forEach { s -> NavigationBarItem(selected=screen==s,onClick={screen=s},icon={Text(s.label.take(1))},label={Text(s.label)}) } } }
    ){pad-> Box(Modifier.padding(pad).fillMaxSize().background(MaterialTheme.colorScheme.background)){ when(screen){
        Screen.TODAY->TodayScreen(c.state)
        Screen.WEEK->WeekScreen(c.state)
        Screen.ANALYSIS->AnalysisScreen(c.state)
        Screen.CHECKIN->CheckInScreen(c.state){c.update(c.state.copy(checkIn=it));Toast.makeText(context,"Check-in сохранён",Toast.LENGTH_SHORT).show()}
        Screen.GOALS->GoalsScreen(c.state){c.update(it);Toast.makeText(context,"Настройки сохранены",Toast.LENGTH_SHORT).show()}
        Screen.RACES->RacesScreen(c.state,{c.update(c.state.copy(races=c.state.races+it))},{id->c.update(c.state.copy(races=c.state.races.filterNot{it.id==id}))})
        Screen.DATA->DataScreen()
    } } }
}
@Composable private fun CardBox(title:String,content:@Composable ColumnScope.()->Unit){ Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=Color.White)){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Text(title,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium);content()}} }
@Composable private fun Metric(label:String,value:String){Column{Text(label,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant);Text(value,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge)}}
@Composable private fun TodayScreen(s:AppState){
    val plan=Planner.plan(s); val score=Planner.recoveryScore(s); val today=plan.find{it.date==LocalDate.now()}?:plan.first()
    LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        item{CardBox("Готовность сегодня"){Text(score?.let{"$it / 100"} ?: "Нет данных",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);Text(if(score==null)"План предварительный. Garmin пока не подключён; проверь самочувствие." else if(score>=75)"Восстановление достаточное. Не добавляем интенсивность." else if(score>=55)"Оставляем лёгкую работу." else "Сократить объём или отдых.")}}
        item{CardBox("Сегодня по плану"){Text(today.title,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Text(today.durationLabel+" · "+today.intensity);Text(today.purpose)}}
        item{CardBox("Восстановление"){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Metric("HRV",s.todayHrv?.let{"$it мс"} ?: "—");Metric("Пульс покоя",s.todayRestingHr?.toString() ?: "—")};Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Metric("Сон",s.todaySleepHours?.let{"$it ч"} ?: "—");Metric("Body Battery",s.todayBodyBattery?.toString() ?: "—")}}}
    }
}
@Composable private fun WeekScreen(s:AppState){
    val plan=Planner.plan(s); val km=Planner.weeklyKmRange(s,plan); val minutes=plan.filter{it.type.startsWith("Бег")}.sumOf{it.minutes}
    LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
        item{CardBox("Неделя"){Text("Объём ${s.trainingBlock.weeklyVolumePct}% · минимум бега ${s.trainingBlock.minRunMinutes} мин");Text("≈ ${km.third} км (${km.first}–${km.second}) · ${(minutes/60.0*10).roundToInt()/10.0} ч");if(Planner.recoveryScore(s)==null)Text("Предварительный план без данных восстановления.")}}
        items(plan){p->CardBox("${p.date.dayOfWeek.name.lowercase().replaceFirstChar{it.uppercase()}} · ${p.date.dayOfMonth}.${p.date.monthValue}"){Text(p.title,fontWeight=FontWeight.Bold);Text(p.durationLabel+" · "+p.intensity);Text(p.purpose,style=MaterialTheme.typography.bodySmall)}}
    }
}
@Composable private fun CheckInScreen(s:AppState,onSave:(CheckIn)->Unit){
    var fatigue by remember(s.checkIn){mutableFloatStateOf(s.checkIn.fatigue.toFloat())};var legs by remember(s.checkIn){mutableFloatStateOf(s.checkIn.legs.toFloat())};var pain by remember(s.checkIn){mutableFloatStateOf(s.checkIn.pain.toFloat())};var mot by remember(s.checkIn){mutableFloatStateOf(s.checkIn.motivation.toFloat())};var note by remember(s.checkIn){mutableStateOf(s.checkIn.note)}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){CardBox("Быстрый check-in"){
        SliderRow("Усталость",fatigue,1f..5f){fatigue=it};SliderRow("Тяжесть ног",legs,1f..5f){legs=it};SliderRow("Боль",pain,0f..10f){pain=it};SliderRow("Желание тренироваться",mot,1f..5f){mot=it};OutlinedTextField(note,{note=it},label={Text("Заметка")},modifier=Modifier.fillMaxWidth());Button(onClick={onSave(CheckIn(fatigue.roundToInt(),legs.roundToInt(),pain.roundToInt(),mot.roundToInt(),note))}){Text("Сохранить")}
    }}
}
@Composable private fun SliderRow(label:String,v:Float,range:ClosedFloatingPointRange<Float>,on:(Float)->Unit){Text("$label: ${v.roundToInt()}");Slider(value=v,onValueChange=on,valueRange=range,steps=((range.endInclusive-range.start).toInt()-1).coerceAtLeast(0))}
@Composable private fun GoalsScreen(s:AppState,onSave:(AppState)->Unit){
    var volume by remember(s.trainingBlock){mutableFloatStateOf(s.trainingBlock.weeklyVolumePct.toFloat())};var minRun by remember(s.trainingBlock){mutableStateOf(s.trainingBlock.minRunMinutes.toString())};var longRun by remember(s.trainingBlock){mutableStateOf(s.trainingBlock.longRunMinutes.toString())};var bike by remember(s.trainingBlock){mutableStateOf(s.trainingBlock.bikeGear)}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){CardBox("Текущий тренировочный блок"){
        Text("Объём этой недели: ${volume.roundToInt()}%",fontWeight=FontWeight.Bold);Slider(value=volume,onValueChange={volume=it},valueRange=40f..100f,steps=11);OutlinedTextField(minRun,{minRun=it.filter(Char::isDigit)},label={Text("Минимальная длительность бега, мин")},modifier=Modifier.fillMaxWidth());OutlinedTextField(longRun,{longRun=it.filter(Char::isDigit)},label={Text("Текущий длительный, мин")},modifier=Modifier.fillMaxWidth());Row(verticalAlignment=Alignment.CenterVertically){Checkbox(bike,{bike=it});Text("Есть одежда для велотренировок")};Button(onClick={val b=s.trainingBlock.copy(weeklyVolumePct=volume.roundToInt(),minRunMinutes=(minRun.toIntOrNull()?:30).coerceIn(15,90),longRunMinutes=(longRun.toIntOrNull()?:75).coerceIn(60,110),bikeGear=bike);onSave(s.copy(trainingBlock=b))}){Text("Сохранить блок")}
    };CardBox("Правила"){Text("Понедельник — отдых. Четверг — силовая без отказа. Длительный ≥60 мин при достаточном восстановлении. Базовый блок — без интервалов и темпа.")}}
}
@Composable private fun RacesScreen(s:AppState,onAdd:(Race)->Unit,onDelete:(String)->Unit){
    var name by remember{mutableStateOf("")};var date by remember{mutableStateOf("")};var dist by remember{mutableStateOf("")};var priority by remember{mutableStateOf("B")}
    LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
        item{CardBox("Добавить забег"){OutlinedTextField(name,{name=it},label={Text("Название")},modifier=Modifier.fillMaxWidth());OutlinedTextField(date,{date=it},label={Text("Дата YYYY-MM-DD")},modifier=Modifier.fillMaxWidth());OutlinedTextField(dist,{dist=it},label={Text("Дистанция, км")},modifier=Modifier.fillMaxWidth());Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("A","B","C").forEach{p->FilterChip(selected=priority==p,onClick={priority=p},label={Text(p)})}};Button(enabled=name.isNotBlank()&&runCatching{LocalDate.parse(date)}.isSuccess&&(dist.toDoubleOrNull()?:0.0)>0,onClick={onAdd(Race(UUID.randomUUID().toString(),name,date,dist,"road",priority));name="";date="";dist=""}){Text("Добавить")}}}
        items(s.races.sortedBy{it.date}){r->CardBox(r.name){Text("${r.date} · ${r.distance} км · приоритет ${r.priority}");if(r.target.isNotBlank())Text(r.target,style=MaterialTheme.typography.bodySmall);TextButton(onClick={onDelete(r.id)}){Text("Удалить")}}}
    }
}
@Composable private fun AnalysisScreen(s:AppState){
    if(s.workouts.isEmpty()){
        Column(Modifier.padding(16.dp)){CardBox("Анализ тренировки"){Text("Нет импортированных тренировок. Garmin-синхронизация ещё не реализована.")}}
        return
    }
    var selected by remember(s.workouts){mutableStateOf(s.workouts.first().id)};val w=s.workouts.find{it.id==selected}?:s.workouts.first();val a=WorkoutAnalyzer.analyze(w)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        Text("Анализ тренировки",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        s.workouts.forEach{x->FilterChip(selected=x.id==selected,onClick={selected=x.id},label={Text("${x.date} · ${x.title}")})}
        CardBox(w.title){Text(a.verdict,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){w.distanceKm?.let{Metric("Дистанция","$it км")};Metric("Длительность","${w.minutes.roundToInt()} мин")};w.avgPace?.let{Text("Темп $it")};if(w.avgHr!=null)Text("Пульс ${w.avgHr} / ${w.maxHr ?: "—"}")}
        CardBox("Что хорошо"){a.good.forEach{Text("• $it")}}
        CardBox("Что поправить"){a.fix.forEach{Text("• $it")}}
        CardBox("Следующая похожая тренировка"){a.next.forEach{Text("• $it")}}
        CardBox("Реакция на следующий день"){Text(a.reaction)}
    }
}
@Composable private fun DataScreen(){Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){CardBox("Garmin"){Text("Не подключён. Интеграция Personal API ещё не загружена в проект.");Text("Персональные данные и ключи не входят в исходный код.")};CardBox("Версия"){Text("Trainwise Android 0.15.0");Text("Kotlin + Jetpack Compose. Не WebView.")}}}
