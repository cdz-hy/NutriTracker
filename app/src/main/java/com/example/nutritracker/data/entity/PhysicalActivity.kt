package com.example.nutritracker.data.entity

/**
 * 物理活动 - 基于 2024 Adult Compendium of Physical Activities
 * MET 值来自 Herrmann et al. (2024)
 */
data class PhysicalActivity(
    val code: String,
    val name: String,
    val description: String,
    val mets: Double,
    val category: ActivityCategory
)

enum class ActivityCategory(val displayName: String) {
    BICYCLING("骑车"),
    CONDITIONING("健身"),
    RUNNING("跑步"),
    SPORT("球类"),
    WALKING("步行"),
    WATER("水上运动"),
    OTHER("其他")
}

/**
 * 常见运动数据库 - 来自 2024 运动代谢当量手册
 */
object PhysicalActivityDatabase {

    fun getAllActivities(): List<PhysicalActivity> = listOf(
        // ── 骑车 ─────────────────────────────────────────────────────────────
        PhysicalActivity("01015", "骑自行车", "一般", 7.5, ActivityCategory.BICYCLING),
        PhysicalActivity("01009", "山地自行车", "一般", 8.5, ActivityCategory.BICYCLING),
        PhysicalActivity("02010", "动感单车", "一般", 7.5, ActivityCategory.BICYCLING),

        // ── 健身 ─────────────────────────────────────────────────────────────
        PhysicalActivity("02030", "徒手健身", "轻到中等强度", 3.5, ActivityCategory.CONDITIONING),
        PhysicalActivity("02020", "徒手健身", "高强度（俯卧撑、仰卧起坐、引体向上）", 7.5, ActivityCategory.CONDITIONING),
        PhysicalActivity("02050", "力量训练", "哑铃、杠铃、器械", 6.0, ActivityCategory.CONDITIONING),
        PhysicalActivity("02055", "力量训练", "高强度，力量举或健美", 6.0, ActivityCategory.CONDITIONING),
        PhysicalActivity("02068", "跳绳", "一般", 12.3, ActivityCategory.CONDITIONING),
        PhysicalActivity("02080", "划船机", "中等强度", 7.0, ActivityCategory.CONDITIONING),
        PhysicalActivity("02090", "椭圆机", "中等强度", 5.0, ActivityCategory.CONDITIONING),
        PhysicalActivity("02095", "楼梯机", "一般", 9.0, ActivityCategory.CONDITIONING),
        PhysicalActivity("02160", "瑜伽", "一般，哈他瑜伽", 3.0, ActivityCategory.CONDITIONING),
        PhysicalActivity("02165", "普拉提", "一般", 3.0, ActivityCategory.CONDITIONING),
        PhysicalActivity("02170", "拉伸", "轻度", 2.3, ActivityCategory.CONDITIONING),
        PhysicalActivity("02210", "HIIT", "中等强度", 7.0, ActivityCategory.CONDITIONING),
        PhysicalActivity("02214", "HIIT", "高强度（波比跳、登山者、深蹲跳）", 11.0, ActivityCategory.CONDITIONING),
        PhysicalActivity("02120", "水中健身", "水中有氧", 5.3, ActivityCategory.CONDITIONING),

        // ── 跑步 ─────────────────────────────────────────────────────────────
        PhysicalActivity("12020", "慢跑", "一般", 7.0, ActivityCategory.RUNNING),
        PhysicalActivity("12150", "跑步", "一般", 8.3, ActivityCategory.RUNNING),
        PhysicalActivity("12180", "跑步机", "一般", 8.0, ActivityCategory.RUNNING),

        // ── 步行 ─────────────────────────────────────────────────────────────
        PhysicalActivity("17160", "步行", "休闲", 3.5, ActivityCategory.WALKING),
        PhysicalActivity("17165", "遛狗", "一般", 3.0, ActivityCategory.WALKING),
        PhysicalActivity("17170", "北欧健走", "一般", 4.8, ActivityCategory.WALKING),
        PhysicalActivity("17080", "徒步", "越野", 6.0, ActivityCategory.WALKING),
        PhysicalActivity("17010", "背包徒步", "一般", 7.0, ActivityCategory.WALKING),

        // ── 球类 ─────────────────────────────────────────────────────────────
        PhysicalActivity("15030", "羽毛球", "社交单双打", 5.5, ActivityCategory.SPORT),
        PhysicalActivity("15055", "篮球", "一般", 6.0, ActivityCategory.SPORT),
        PhysicalActivity("15100", "拳击", "比赛", 12.8, ActivityCategory.SPORT),
        PhysicalActivity("15110", "拳击", "沙袋训练", 5.5, ActivityCategory.SPORT),
        PhysicalActivity("15230", "足球", "一般", 8.0, ActivityCategory.SPORT),
        PhysicalActivity("15255", "高尔夫", "一般", 4.8, ActivityCategory.SPORT),
        PhysicalActivity("15320", "手球", "一般", 12.0, ActivityCategory.SPORT),
        PhysicalActivity("15360", "冰球", "一般", 8.0, ActivityCategory.SPORT),
        PhysicalActivity("15425", "武术", "慢速", 5.3, ActivityCategory.SPORT),
        PhysicalActivity("15430", "武术", "中等速度（柔道、空手道、跆拳道）", 10.3, ActivityCategory.SPORT),
        PhysicalActivity("15530", "壁球", "一般", 7.0, ActivityCategory.SPORT),
        PhysicalActivity("15551", "跳绳", "中等速度", 11.8, ActivityCategory.SPORT),
        PhysicalActivity("15560", "橄榄球", "竞技", 8.3, ActivityCategory.SPORT),
        PhysicalActivity("15610", "足球", "休闲", 7.0, ActivityCategory.SPORT),
        PhysicalActivity("15620", "棒球/垒球", "一般", 5.0, ActivityCategory.SPORT),
        PhysicalActivity("15652", "壁球", "一般", 7.3, ActivityCategory.SPORT),
        PhysicalActivity("15660", "乒乓球", "一般", 4.0, ActivityCategory.SPORT),
        PhysicalActivity("15670", "太极拳/气功", "一般", 3.5, ActivityCategory.SPORT),
        PhysicalActivity("15675", "网球", "一般", 7.3, ActivityCategory.SPORT),
        PhysicalActivity("15710", "排球", "非竞技", 4.0, ActivityCategory.SPORT),
        PhysicalActivity("15730", "摔跤", "一般", 6.0, ActivityCategory.SPORT),
        PhysicalActivity("15740", "匹克球", "一般", 4.8, ActivityCategory.SPORT),

        // ── 水上运动 ─────────────────────────────────────────────────────────
        PhysicalActivity("18350", "游泳", "踩水，中等强度", 3.5, ActivityCategory.WATER),
        PhysicalActivity("18100", "皮划艇", "中等强度", 5.0, ActivityCategory.WATER),
        PhysicalActivity("18220", "冲浪", "一般", 3.0, ActivityCategory.WATER),
        PhysicalActivity("18210", "浮潜", "一般", 5.0, ActivityCategory.WATER),
        PhysicalActivity("18225", "桨板", "站立", 6.0, ActivityCategory.WATER),
        PhysicalActivity("18360", "水球", "一般", 10.0, ActivityCategory.WATER),

        // ── 其他 ─────────────────────────────────────────────────────────────
        PhysicalActivity("03015", "有氧舞蹈", "一般", 7.3, ActivityCategory.OTHER),
        PhysicalActivity("15580", "滑板", "一般", 5.0, ActivityCategory.OTHER),
        PhysicalActivity("15590", "轮滑", "一般", 7.0, ActivityCategory.OTHER),
        PhysicalActivity("15592", "轮滑鞋", "休闲速度", 7.5, ActivityCategory.OTHER),
        PhysicalActivity("19030", "滑冰", "一般", 7.0, ActivityCategory.OTHER),
        PhysicalActivity("19075", "滑雪", "一般", 7.0, ActivityCategory.OTHER),
        PhysicalActivity("19080", "越野滑雪", "一般", 7.0, ActivityCategory.OTHER),
        PhysicalActivity("99999", "自定义活动", "手动输入卡路里", 0.0, ActivityCategory.OTHER)
    )

    fun getActivitiesByCategory(): Map<ActivityCategory, List<PhysicalActivity>> =
        getAllActivities().groupBy { it.category }

    fun searchActivities(query: String): List<PhysicalActivity> {
        if (query.isBlank()) return getAllActivities().filter { it.code != "99999" }
        return getAllActivities().filter {
            it.code != "99999" && (
                it.name.contains(query, ignoreCase = true) ||
                it.description.contains(query, ignoreCase = true) ||
                it.category.displayName.contains(query, ignoreCase = true)
            )
        }
    }

    fun getActivityByCode(code: String): PhysicalActivity? =
        getAllActivities().find { it.code == code }
}
