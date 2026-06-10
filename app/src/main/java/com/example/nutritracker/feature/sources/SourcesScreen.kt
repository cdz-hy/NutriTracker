package com.example.nutritracker.feature.sources

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nutritracker.ui.theme.*
import kotlinx.coroutines.delay

data class SourceLink(
    val citation: String,
    val url: String
)

data class SourceEntry(
    val title: String,
    val description: String,
    val links: List<SourceLink>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourcesScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val entries = remember {
        listOf(
            SourceEntry(
                title = "能量需求 (TDEE、基础代谢与活动水平)",
                description = "每日卡路里目标、基础代谢率以及身体活动系数均采用美国医学研究所 (Institute of Medicine) 的方程。来源：Institute of Medicine (2005). Dietary Reference Intakes for Energy, Carbohydrate, Fiber, Fat, Fatty Acids, Cholesterol, Protein, and Amino Acids，第 5 章及表 5-5。",
                links = listOf(
                    SourceLink(
                        citation = "Institute of Medicine (2005). Dietary Reference Intakes for Energy, Carbohydrate, Fiber, Fat, Fatty Acids, Cholesterol, Protein, and Amino Acids. National Academies Press.",
                        url = "https://nap.nationalacademies.org/catalog/10490"
                    )
                )
            ),
            SourceEntry(
                title = "身体质量指数 (BMI)",
                description = "BMI 等于体重（千克）除以身高（米）的平方。健康分类（体重过低、正常体重、超重前期、I–III 级肥胖）遵循世界卫生组织成人 BMI 分类标准。",
                links = listOf(
                    SourceLink(
                        citation = "World Health Organization. Body mass index (BMI), adult classification. WHO Global Health Observatory.",
                        url = "https://www.who.int/data/gho/data/themes/topics/topic-details/GHO/body-mass-index"
                    )
                )
            ),
            SourceEntry(
                title = "宏量营养素分配",
                description = "默认的 60% 碳水化合物、25% 脂肪、15% 蛋白质比例落在 WHO 推荐的人群营养摄入范围内。您可以在 设置 → 宏量分配 中调节。来源：WHO Technical Report Series 916 (2003), Diet, Nutrition and the Prevention of Chronic Diseases。",
                links = listOf(
                    SourceLink(
                        citation = "World Health Organization (2003). Diet, Nutrition and the Prevention of Chronic Diseases. WHO Technical Report Series 916.",
                        url = "https://iris.who.int/handle/10665/42665"
                    )
                )
            ),
            SourceEntry(
                title = "活动消耗的卡路里 (MET 数值)",
                description = "活动中消耗的卡路里按 MET × 体重（千克）× 时长（小时）估算，所用数值来自 Adult Compendium of Physical Activities 运动代谢当量对照表。",
                links = listOf(
                    SourceLink(
                        citation = "Herrmann SD, et al. (2024). 2024 Adult Compendium of Physical Activities. Journal of Sport and Health Science.",
                        url = "https://pubmed.ncbi.nlm.nih.gov/38242596/"
                    )
                )
            ),
            SourceEntry(
                title = "非二元性别人士的卡路里估算",
                description = "能量消耗的研究历史上一直仅采用二元性别分类，因此目前并不存在一条经过验证的、适用于非二元性别人士的 TDEE 公式。NutriTracker 在此提供平均参考、雌激素型参考、睾酮型参考三种选项。如果精确数值对您确实重要，请咨询了解您激素状况的临床医师。",
                links = listOf(
                    SourceLink(
                        citation = "Linsenmeyer W, Waters J (2021). Sex and gender differences in nutrition research: considerations with the transgender and gender nonconforming population. Nutrition Journal, 20:6.",
                        url = "https://doi.org/10.1186/s12937-021-00662-z"
                    ),
                    SourceLink(
                        citation = "Wiik A, et al. (2018). Metabolic and functional changes in transgender individuals following cross-sex hormone treatment. Contemporary Clinical Trials Communications, 10:148–153.",
                        url = "https://pmc.ncbi.nlm.nih.gov/articles/PMC6046513/"
                    ),
                    SourceLink(
                        citation = "Linsenmeyer W, Drallmeier T, Thomure M (2020). Towards gender-affirming nutrition assessment: adult transgender men with distinct nutrition considerations. Nutrition Journal, 19:74.",
                        url = "https://link.springer.com/article/10.1186/s12937-020-00590-4"
                    )
                )
            ),
            SourceEntry(
                title = "营养参考摄入量",
                description = "日记营养面板中显示的每日参考量来自美国医学研究所(Institute of Medicine)的膳食参考摄入量(DRI)汇总报告，涵盖成人各项微量营养素目标。",
                links = listOf(
                    SourceLink(
                        citation = "Institute of Medicine. Dietary Reference Intakes: The Essential Guide to Nutrient Requirements. Summary Report. National Academies Press.",
                        url = "https://www.nationalacademies.org/our-work/summary-report-of-the-dietary-reference-intakes"
                    )
                )
            )
        )
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "科学文献来源与依据",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Dimens.ContentPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.CardSpacing),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Text(
                    text = "NutriTracker 的每一项计算都基于权威的、经同行评审的临床研究成果。您可以在此查阅每项公式和基准的数据出处，并自行前往原始文献求证。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            itemsIndexed(entries) { index, entry ->
                StaggeredFadeIn(index = index) {
                    SourceCard(
                        entry = entry,
                        onOpenUrl = { url ->
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                // Fallback
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceCard(
    entry: SourceEntry,
    onOpenUrl: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = Dimens.CardElevation),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .padding(Dimens.CardInnerPadding)
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 标题行（始终显示）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "折叠" else "展开",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(Dimens.IconSizeMedium)
                )
            }

            // 可折叠详情
            if (expanded) {
                Text(
                    text = entry.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                entry.links.forEach { link ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = link.citation,
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )

                        TextButton(
                            onClick = { onOpenUrl(link.url) },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                            modifier = Modifier.align(Alignment.Start)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "打开文献来源",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
