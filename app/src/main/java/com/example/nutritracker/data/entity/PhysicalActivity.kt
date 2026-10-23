package com.example.nutritracker.data.entity

/**
 * Physical activity catalog based on the 2024 Adult Compendium of Physical Activities (Herrmann et al., 2024).
 */
data class PhysicalActivity(
    val code: String,
    val name: String,
    val description: String,
    val mets: Double,
    val category: ActivityCategory,
    val nameEn: String = name,
    val descriptionEn: String = description
) {
    fun displayName(isZh: Boolean): String = if (isZh) name else nameEn
    fun displayDescription(isZh: Boolean): String = if (isZh) description else descriptionEn
}

enum class ActivityCategory(val displayName: String, val displayNameEn: String) {
    BICYCLING("骑车", "Cycling"),
    CONDITIONING("健身", "Conditioning"),
    RUNNING("跑步", "Running"),
    SPORT("球类", "Sports"),
    WALKING("步行", "Walking"),
    WATER("水上运动", "Water Sports"),
    OTHER("其他", "Other");

    fun label(isZh: Boolean): String = if (isZh) displayName else displayNameEn
}

/**
 * Built-in physical activity database from the 2024 Compendium.
 */
object PhysicalActivityDatabase {

    fun getAllActivities(): List<PhysicalActivity> = listOf(
        // Cycling
        PhysicalActivity("01015", "骑自行车", "一般", 7.5, ActivityCategory.BICYCLING, "Bicycling", "General"),
        PhysicalActivity("01009", "山地自行车", "一般", 8.5, ActivityCategory.BICYCLING, "Mountain Biking", "General"),
        PhysicalActivity("02010", "动感单车", "一般", 7.5, ActivityCategory.BICYCLING, "Stationary Cycling", "Spinning / moderate-vigorous"),

        // Conditioning
        PhysicalActivity("02030", "徒手健身", "轻到中等强度", 3.5, ActivityCategory.CONDITIONING, "Calisthenics", "Light-to-moderate effort"),
        PhysicalActivity("02020", "徒手健身", "高强度（俯卧撑、仰卧起坐、引体向上）", 7.5, ActivityCategory.CONDITIONING, "Calisthenics", "Vigorous (push-ups, sit-ups, pull-ups)"),
        PhysicalActivity("02050", "力量训练", "哑铃、杠铃、器械", 6.0, ActivityCategory.CONDITIONING, "Strength Training", "Free weights or machines"),
        PhysicalActivity("02055", "力量训练", "高强度，力量举或健美", 6.0, ActivityCategory.CONDITIONING, "Resistance Training", "Vigorous powerlifting or bodybuilding"),
        PhysicalActivity("02068", "跳绳", "快速/高强度", 12.3, ActivityCategory.CONDITIONING, "Jump Rope", "Fast / vigorous pace"),
        PhysicalActivity("02080", "划船机", "中等强度", 7.0, ActivityCategory.CONDITIONING, "Rowing Machine", "Moderate effort"),
        PhysicalActivity("02090", "椭圆机", "中等强度", 5.0, ActivityCategory.CONDITIONING, "Elliptical Trainer", "Moderate effort"),
        PhysicalActivity("02095", "楼梯机", "一般", 9.0, ActivityCategory.CONDITIONING, "Stair Stepper", "General"),
        PhysicalActivity("02160", "瑜伽", "一般，哈他瑜伽", 3.0, ActivityCategory.CONDITIONING, "Yoga", "Hatha / general"),
        PhysicalActivity("02165", "普拉提", "一般", 3.0, ActivityCategory.CONDITIONING, "Pilates", "General"),
        PhysicalActivity("02170", "拉伸", "轻度", 2.3, ActivityCategory.CONDITIONING, "Stretching", "Mild flexibility work"),
        PhysicalActivity("02210", "HIIT", "中等强度", 7.0, ActivityCategory.CONDITIONING, "HIIT", "Moderate interval training"),
        PhysicalActivity("02214", "HIIT", "高强度（波比跳、登山者、深蹲跳）", 11.0, ActivityCategory.CONDITIONING, "HIIT", "Vigorous (burpees, climbers, jump squats)"),
        PhysicalActivity("02120", "水中健身", "水中有氧", 5.3, ActivityCategory.CONDITIONING, "Water Aerobics", "Aquatic fitness"),

        // Running
        PhysicalActivity("12020", "慢跑", "一般", 7.0, ActivityCategory.RUNNING, "Jogging", "General"),
        PhysicalActivity("12150", "跑步", "一般", 8.3, ActivityCategory.RUNNING, "Running", "General"),
        PhysicalActivity("12180", "跑步机", "一般", 8.0, ActivityCategory.RUNNING, "Treadmill Running", "General"),

        // Walking
        PhysicalActivity("17160", "步行", "休闲", 3.5, ActivityCategory.WALKING, "Walking", "Pleasure / brisk leisure"),
        PhysicalActivity("17165", "遛狗", "一般", 3.0, ActivityCategory.WALKING, "Walking the Dog", "General"),
        PhysicalActivity("17170", "北欧健走", "一般", 4.8, ActivityCategory.WALKING, "Nordic Walking", "General"),
        PhysicalActivity("17080", "徒步", "越野", 6.0, ActivityCategory.WALKING, "Hiking", "Cross-country"),
        PhysicalActivity("17010", "背包徒步", "一般", 7.0, ActivityCategory.WALKING, "Backpacking", "General"),

        // Sports
        PhysicalActivity("15030", "羽毛球", "社交单双打", 5.5, ActivityCategory.SPORT, "Badminton", "Social singles or doubles"),
        PhysicalActivity("15055", "篮球", "一般", 6.0, ActivityCategory.SPORT, "Basketball", "General"),
        PhysicalActivity("15100", "拳击", "比赛", 12.8, ActivityCategory.SPORT, "Boxing", "In ring / spar"),
        PhysicalActivity("15110", "拳击", "沙袋训练", 5.5, ActivityCategory.SPORT, "Boxing", "Punching bag"),
        PhysicalActivity("15230", "足球", "一般", 8.0, ActivityCategory.SPORT, "Soccer / Football", "General"),
        PhysicalActivity("15610", "足球", "休闲", 7.0, ActivityCategory.SPORT, "Soccer / Football", "Casual"),
        PhysicalActivity("15255", "高尔夫", "一般", 4.8, ActivityCategory.SPORT, "Golf", "General"),
        PhysicalActivity("15320", "手球", "一般", 12.0, ActivityCategory.SPORT, "Handball", "General"),
        PhysicalActivity("15360", "冰球", "一般", 8.0, ActivityCategory.SPORT, "Ice Hockey", "General"),
        PhysicalActivity("15425", "武术", "慢速", 5.3, ActivityCategory.SPORT, "Martial Arts", "Slower pace"),
        PhysicalActivity("15430", "武术", "中等速度（柔道、空手道、跆拳道）", 10.3, ActivityCategory.SPORT, "Martial Arts", "Moderate pace (judo, karate, taekwondo)"),
        PhysicalActivity("15652", "壁球", "一般", 7.3, ActivityCategory.SPORT, "Squash / Racquetball", "General"),
        PhysicalActivity("15560", "橄榄球", "竞技", 8.3, ActivityCategory.SPORT, "Rugby / Football", "Competitive"),
        PhysicalActivity("15620", "棒球/垒球", "一般", 5.0, ActivityCategory.SPORT, "Baseball / Softball", "General"),
        PhysicalActivity("15660", "乒乓球", "一般", 4.0, ActivityCategory.SPORT, "Table Tennis", "General"),
        PhysicalActivity("15670", "太极拳/气功", "一般", 3.5, ActivityCategory.SPORT, "Tai Chi / Qigong", "General"),
        PhysicalActivity("15675", "网球", "一般", 7.3, ActivityCategory.SPORT, "Tennis", "General"),
        PhysicalActivity("15710", "排球", "非竞技", 4.0, ActivityCategory.SPORT, "Volleyball", "Non-competitive"),
        PhysicalActivity("15730", "摔跤", "一般", 6.0, ActivityCategory.SPORT, "Wrestling", "General"),
        PhysicalActivity("15740", "匹克球", "一般", 4.8, ActivityCategory.SPORT, "Pickleball", "General"),

        // Water sports
        PhysicalActivity("18350", "游泳", "踩水，中等强度", 3.5, ActivityCategory.WATER, "Swimming", "Treading water, moderate effort"),
        PhysicalActivity("18100", "皮划艇", "中等强度", 5.0, ActivityCategory.WATER, "Kayaking", "Moderate effort"),
        PhysicalActivity("18220", "冲浪", "一般", 3.0, ActivityCategory.WATER, "Surfing", "General"),
        PhysicalActivity("18210", "浮潜", "一般", 5.0, ActivityCategory.WATER, "Snorkeling", "General"),
        PhysicalActivity("18225", "桨板", "站立", 6.0, ActivityCategory.WATER, "Paddleboarding", "Standing"),
        PhysicalActivity("18360", "水球", "一般", 10.0, ActivityCategory.WATER, "Water Polo", "General"),

        // Other
        PhysicalActivity("03015", "有氧舞蹈", "一般", 7.3, ActivityCategory.OTHER, "Aerobic Dance", "General"),
        PhysicalActivity("15580", "滑板", "一般", 5.0, ActivityCategory.OTHER, "Skateboarding", "General"),
        PhysicalActivity("15590", "轮滑", "一般", 7.0, ActivityCategory.OTHER, "Roller Skating", "General"),
        PhysicalActivity("15592", "轮滑鞋", "休闲速度", 7.5, ActivityCategory.OTHER, "Inline Skating", "Casual pace"),
        PhysicalActivity("19030", "滑冰", "一般", 7.0, ActivityCategory.OTHER, "Ice Skating", "General"),
        PhysicalActivity("19075", "滑雪", "一般", 7.0, ActivityCategory.OTHER, "Skiing", "General"),
        PhysicalActivity("19080", "越野滑雪", "一般", 7.0, ActivityCategory.OTHER, "Cross-Country Skiing", "General")
    )

    fun searchActivities(query: String): List<PhysicalActivity> {
        if (query.isBlank()) return getAllActivities()
        return getAllActivities().filter {
            it.name.contains(query, ignoreCase = true) ||
                it.description.contains(query, ignoreCase = true) ||
                it.category.displayName.contains(query, ignoreCase = true) ||
                it.nameEn.contains(query, ignoreCase = true) ||
                it.descriptionEn.contains(query, ignoreCase = true) ||
                it.category.displayNameEn.contains(query, ignoreCase = true)
        }
    }
}
