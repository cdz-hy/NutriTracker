package com.example.nutritracker.feature.meal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nutritracker.data.dao.RecipeWithIngredients
import com.example.nutritracker.data.entity.IntakeType
import com.example.nutritracker.data.entity.Meal
import com.example.nutritracker.ui.components.mealTypeLabel
import com.example.nutritracker.ui.theme.Dimens
import com.example.nutritracker.util.LocalIsChinese
import com.example.nutritracker.util.UnitCalc
import com.example.nutritracker.util.tr
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodLibraryScreen(
    onBack: () -> Unit,
    onEditMealTemplate: (Long) -> Unit,
    vm: FoodLibraryViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val query by vm.query.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    val meals by vm.filteredMeals.collectAsStateWithLifecycle()
    val allMeals by vm.allLibraryMeals.collectAsStateWithLifecycle()
    val recipes by vm.recipes.collectAsStateWithLifecycle()
    val useKj by vm.useKj.collectAsStateWithLifecycle()
    val isZh = LocalIsChinese.current

    var logMealTarget by remember { mutableStateOf<Meal?>(null) }
    var logRecipeTarget by remember { mutableStateOf<RecipeWithIngredients?>(null) }
    var editingRecipe by remember { mutableStateOf<RecipeWithIngredients?>(null) }
    var showCreateRecipe by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        vm.toastEvent.collect { msg -> snackbarHostState.showSnackbar(msg) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(tr("食物库与自制食谱", "Food Library & Recipes"), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("返回", "Back"))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            if (selectedTab == 1) {
                ExtendedFloatingActionButton(
                    onClick = { showCreateRecipe = true },
                    icon = { Icon(Icons.Filled.Add, null) },
                    text = { Text(tr("新建食谱", "New Recipe")) }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text(tr("食物库 (${meals.size})", "Foods (${meals.size})")) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text(tr("自制食谱 (${recipes.size})", "Recipes (${recipes.size})")) })
            }

            OutlinedTextField(
                value = query,
                onValueChange = { vm.setQuery(it) },
                placeholder = { Text(if (selectedTab == 0) tr("搜索食物名称...", "Search foods...") else tr("搜索食谱...", "Search recipes...")) },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { vm.setQuery("") }) {
                            Icon(Icons.Filled.Clear, tr("清除", "Clear"))
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.ContentPadding, vertical = 10.dp)
            )

            if (selectedTab == 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = Dimens.ContentPadding),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LibraryFilter.entries.forEach { f ->
                        FilterChip(
                            selected = filter == f,
                            onClick = { vm.setFilter(f) },
                            label = { Text(f.displayLabel(isZh)) }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                if (meals.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(tr("暂无匹配食物", "No matching foods"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = Dimens.ContentPadding, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(meals, key = { it.id }) { meal ->
                            LibraryMealItem(
                                meal = meal,
                                useKj = useKj,
                                onToggleFavorite = { vm.toggleFavorite(meal) },
                                onEdit = { onEditMealTemplate(meal.id) },
                                onDelete = { vm.deleteOrArchiveMeal(meal) },
                                onQuickLog = { logMealTarget = meal }
                            )
                        }
                    }
                }
            } else {
                if (recipes.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.RestaurantMenu, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(tr("还没有自制食谱", "No custom recipes yet"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                tr("点击右下角把常吃食材组合为固定套餐", "Tap + to combine favorite foods into a meal preset"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = Dimens.ContentPadding, end = Dimens.ContentPadding, top = 4.dp, bottom = 84.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(recipes, key = { it.recipe.id }) { recipe ->
                            RecipeItemCard(
                                recipe = recipe,
                                useKj = useKj,
                                onLog = { logRecipeTarget = recipe },
                                onEdit = { editingRecipe = recipe },
                                onDelete = { vm.deleteRecipe(recipe.recipe.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    logMealTarget?.let { meal ->
        var chosenType by remember { mutableStateOf(vm.defaultIntakeType) }
        var gramsStr by remember { mutableStateOf((meal.portionGrams ?: 100.0).roundToInt().toString()) }
        val grams = gramsStr.toDoubleOrNull() ?: 0.0
        AlertDialog(
            onDismissRequest = { logMealTarget = null },
            title = { Text(tr("记入 ${meal.name}", "Log ${meal.name}")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IntakeType.entries.forEach { t ->
                            FilterChip(selected = chosenType == t, onClick = { chosenType = t }, label = { Text(mealTypeLabel(t)) })
                        }
                    }
                    OutlinedTextField(
                        value = gramsStr,
                        onValueChange = { gramsStr = it },
                        label = { Text(tr("食用克重 (g)", "Serving weight (g)")) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = tr(
                            "预计：${UnitCalc.formatEnergy(meal.energyKcal100 * grams / 100.0, useKj)}",
                            "Estimated: ${UnitCalc.formatEnergy(meal.energyKcal100 * grams / 100.0, useKj)}"
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { vm.logMeal(meal, grams, chosenType); logMealTarget = null },
                    enabled = grams > 0.0
                ) { Text(tr("添加", "Add")) }
            },
            dismissButton = { TextButton(onClick = { logMealTarget = null }) { Text(tr("取消", "Cancel")) } }
        )
    }

    logRecipeTarget?.let { r ->
        var chosenType by remember { mutableStateOf(vm.defaultIntakeType) }
        var servingsStr by remember { mutableStateOf(r.recipe.defaultServings.toString()) }
        val servings = servingsStr.toDoubleOrNull() ?: 0.0
        AlertDialog(
            onDismissRequest = { logRecipeTarget = null },
            title = { Text(tr("记入食谱：${r.recipe.name}", "Log Recipe: ${r.recipe.name}")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IntakeType.entries.forEach { t ->
                            FilterChip(selected = chosenType == t, onClick = { chosenType = t }, label = { Text(mealTypeLabel(t)) })
                        }
                    }
                    OutlinedTextField(
                        value = servingsStr,
                        onValueChange = { servingsStr = it },
                        label = { Text(tr("食用份数", "Servings")) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { vm.logRecipe(r, servings, chosenType); logRecipeTarget = null },
                    enabled = servings > 0.0
                ) { Text(tr("记入", "Log")) }
            },
            dismissButton = { TextButton(onClick = { logRecipeTarget = null }) { Text(tr("取消", "Cancel")) } }
        )
    }

    if (showCreateRecipe || editingRecipe != null) {
        RecipeEditorDialog(
            existing = editingRecipe,
            availableMeals = allMeals,
            useKj = useKj,
            onDismiss = { showCreateRecipe = false; editingRecipe = null },
            onSave = { name, desc, servings, ings ->
                vm.saveRecipe(editingRecipe?.recipe?.id ?: 0L, name, desc, servings, ings)
                showCreateRecipe = false
                editingRecipe = null
            }
        )
    }
}

@Composable
private fun LibraryMealItem(
    meal: Meal,
    useKj: Boolean,
    onToggleFavorite: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onQuickLog: () -> Unit
) {
    var confirmDelete by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onQuickLog),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggleFavorite, modifier = Modifier.size(34.dp)) {
                Icon(
                    if (meal.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = tr("收藏", "Favorite"),
                    tint = if (meal.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(meal.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(
                    text = tr(
                        "每100g: ${UnitCalc.formatEnergy(meal.energyKcal100, useKj)} · 碳${meal.carbohydrates100.roundToInt()} 脂${meal.fat100.roundToInt()} 蛋${meal.proteins100.roundToInt()}",
                        "Per 100g: ${UnitCalc.formatEnergy(meal.energyKcal100, useKj)} · C ${meal.carbohydrates100.roundToInt()} F ${meal.fat100.roundToInt()} P ${meal.proteins100.roundToInt()}"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onQuickLog, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Filled.AddCircleOutline, contentDescription = tr("记入", "Log"), tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Filled.Edit, contentDescription = tr("编辑", "Edit"), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = { confirmDelete = true }, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Filled.DeleteOutline, contentDescription = tr("删除", "Delete"), tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(tr("移除食物", "Remove Food")) },
            text = {
                Text(
                    tr(
                        "若该食物没有历史记录引用将被彻底删除；否则会从食物库隐藏且保留历史记录。",
                        "Unused items will be deleted permanently; referenced items will be archived from the library while keeping past logs."
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { onDelete(); confirmDelete = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(tr("确定", "Confirm")) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(tr("取消", "Cancel")) } }
        )
    }
}

@Composable
private fun RecipeItemCard(
    recipe: RecipeWithIngredients,
    useKj: Boolean,
    onLog: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var confirmDelete by remember { mutableStateOf(false) }
    val totalKcal = recipe.ingredients.sumOf { it.meal.energyKcal100 * it.ingredient.amountGrams / 100.0 }
    val totalWeight = recipe.ingredients.sumOf { it.ingredient.amountGrams }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onLog),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(recipe.recipe.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = tr(
                            "${recipe.ingredients.size} 种食材 · 共 ${totalWeight.roundToInt()}g · ${UnitCalc.formatEnergy(totalKcal, useKj)}",
                            "${recipe.ingredients.size} items · ${totalWeight.roundToInt()}g total · ${UnitCalc.formatEnergy(totalKcal, useKj)}"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row {
                    IconButton(onClick = onLog, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Filled.AddCircle, tr("一键记入", "Quick Log"), tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Filled.Edit, tr("编辑", "Edit"), modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { confirmDelete = true }, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Filled.DeleteOutline, tr("删除", "Delete"), tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }
            Text(
                text = recipe.ingredients.joinToString(" · ") { "${it.meal.name} ${it.ingredient.amountGrams.roundToInt()}g" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(tr("删除食谱", "Delete Recipe")) },
            text = { Text(tr("确定删除食谱 \"${recipe.recipe.name}\" 吗？", "Delete recipe \"${recipe.recipe.name}\"?")) },
            confirmButton = {
                TextButton(
                    onClick = { onDelete(); confirmDelete = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(tr("删除", "Delete")) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(tr("取消", "Cancel")) } }
        )
    }
}

@Composable
private fun RecipeEditorDialog(
    existing: RecipeWithIngredients?,
    availableMeals: List<Meal>,
    useKj: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, desc: String, defaultServings: Double, ingredients: List<Pair<Long, Double>>) -> Unit
) {
    var name by remember { mutableStateOf(existing?.recipe?.name ?: "") }
    var desc by remember { mutableStateOf(existing?.recipe?.description ?: "") }
    var servingsStr by remember { mutableStateOf((existing?.recipe?.defaultServings ?: 1.0).toString()) }
    var ingredients by remember {
        mutableStateOf<List<Pair<Meal, String>>>(
            existing?.ingredients?.map { it.meal to it.ingredient.amountGrams.roundToInt().toString() } ?: emptyList()
        )
    }
    var pickMealQuery by remember { mutableStateOf("") }
    var showMealPicker by remember { mutableStateOf(false) }

    val totalKcal = ingredients.sumOf { (meal, gStr) ->
        meal.energyKcal100 * (gStr.toDoubleOrNull() ?: 0.0) / 100.0
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (existing == null) tr("新建食谱", "New Recipe") else tr("编辑食谱", "Edit Recipe"),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(tr("食谱/套餐名称", "Recipe name")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text(tr("备注(可选)", "Notes (optional)")) },
                        singleLine = true,
                        modifier = Modifier.weight(1.4f)
                    )
                    OutlinedTextField(
                        value = servingsStr,
                        onValueChange = { servingsStr = it },
                        label = { Text(tr("默认份数", "Servings")) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = tr(
                        "食材清单 (${ingredients.size}项 · 总计 ${UnitCalc.formatEnergy(totalKcal, useKj)})",
                        "Ingredients (${ingredients.size} · Total ${UnitCalc.formatEnergy(totalKcal, useKj)})"
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                ingredients.forEachIndexed { idx, (meal, gStr) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(meal.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        OutlinedTextField(
                            value = gStr,
                            onValueChange = { newG ->
                                ingredients = ingredients.toMutableList().apply { set(idx, meal to newG) }
                            },
                            label = { Text("g") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.width(82.dp)
                        )
                        IconButton(
                            onClick = { ingredients = ingredients.filterIndexed { i, _ -> i != idx } },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Filled.Close, tr("移除", "Remove"), tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                OutlinedButton(
                    onClick = { showMealPicker = !showMealPicker },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (showMealPicker) tr("收起食物库选择", "Hide library picker") else tr("从食物库添加食材", "Add Ingredient from Library"))
                }

                if (showMealPicker) {
                    OutlinedTextField(
                        value = pickMealQuery,
                        onValueChange = { pickMealQuery = it },
                        placeholder = { Text(tr("筛选食材...", "Filter ingredients...")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    val matches = remember(availableMeals, pickMealQuery) {
                        val q = pickMealQuery.trim()
                        if (q.isEmpty()) availableMeals.take(10)
                        else availableMeals.filter { it.name.contains(q, ignoreCase = true) }.take(10)
                    }
                    matches.forEach { m ->
                        ListItem(
                            headlineContent = { Text(m.name) },
                            supportingContent = { Text("${UnitCalc.formatEnergy(m.energyKcal100, useKj)} / 100g") },
                            trailingContent = { Icon(Icons.Filled.Add, null) },
                            modifier = Modifier.clickable {
                                ingredients = ingredients + (m to (m.portionGrams ?: 100.0).roundToInt().toString())
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            val validPairs = ingredients.mapNotNull { (m, g) ->
                val gv = g.toDoubleOrNull()
                if (gv != null && gv > 0) m.id to gv else null
            }
            Button(
                onClick = {
                    onSave(name, desc, servingsStr.toDoubleOrNull() ?: 1.0, validPairs)
                },
                enabled = name.isNotBlank() && validPairs.isNotEmpty()
            ) { Text(tr("保存食谱", "Save Recipe")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("取消", "Cancel")) } },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}
