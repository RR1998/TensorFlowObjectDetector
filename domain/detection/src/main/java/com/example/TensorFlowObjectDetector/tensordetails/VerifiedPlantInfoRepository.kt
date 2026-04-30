package com.example.TensorFlowObjectDetector.tensordetails

class VerifiedPlantInfoRepository {

    private val entriesByLabel = listOf(
        VerifiedPlantInfo(
            commonName = "Daisy",
            supportedLabels = setOf("daisy", "daisies"),
            scientificName = "Bellis perennis",
            description = "A daisy is a flowering plant in the aster family with ray florets surrounding a yellow central disc.",
            sourceAttribution = "Paraphrased from Encyclopaedia Britannica: Daisy"
        ),
        VerifiedPlantInfo(
            commonName = "Dandelion",
            supportedLabels = setOf("dandelion", "dandelions"),
            scientificName = "Taraxacum officinale",
            description = "A dandelion is a perennial herb of the aster family, known for its yellow flower heads and wind-dispersed seeds.",
            sourceAttribution = "Paraphrased from Encyclopaedia Britannica: Dandelion"
        ),
        VerifiedPlantInfo(
            commonName = "Sunflower",
            supportedLabels = setOf("sunflower", "sunflowers"),
            scientificName = "Helianthus annuus",
            description = "A sunflower is an annual herb grown for its large flower head and edible oil-rich seeds.",
            sourceAttribution = "Paraphrased from Encyclopaedia Britannica: Sunflower"
        ),
        VerifiedPlantInfo(
            commonName = "Orchid",
            supportedLabels = setOf("orchid"),
            scientificName = "Orchidaceae",
            description = "An orchid is a member of a large flowering plant family known for diverse, often showy blooms.",
            sourceAttribution = "Paraphrased from Encyclopaedia Britannica: Orchid"
        ),
        VerifiedPlantInfo(
            commonName = "Rose",
            supportedLabels = setOf("rose", "roses"),
            scientificName = "Rosa",
            description = "A rose is a perennial shrub in the genus Rosa, commonly cultivated for fragrant and colorful flowers.",
            sourceAttribution = "Paraphrased from Encyclopaedia Britannica: Rose"
        ),
        VerifiedPlantInfo(
            commonName = "Tulip",
            supportedLabels = setOf("tulip", "tulips"),
            scientificName = "Tulipa",
            description = "A tulip is a bulbous herb in the lily family grown widely in temperate regions for its showy bloom.",
            sourceAttribution = "Paraphrased from Encyclopaedia Britannica: Tulip"
        ),
        VerifiedPlantInfo(
            commonName = "Lily",
            supportedLabels = setOf("lily"),
            scientificName = "Lilium",
            description = "A lily is a herbaceous flowering plant in the genus Lilium, valued as an ornamental and often grown from bulbs.",
            sourceAttribution = "Paraphrased from Encyclopaedia Britannica: Lily"
        ),
        VerifiedPlantInfo(
            commonName = "Lavender",
            supportedLabels = setOf("lavender"),
            scientificName = "Lavandula",
            description = "Lavender is a Mediterranean plant in the mint family, known for fragrant leaves and flowers used for essential oils.",
            sourceAttribution = "Paraphrased from Encyclopaedia Britannica: Lavender"
        )
    ).flatMap { entry ->
        entry.supportedLabels.map { label -> label to entry }
    }.toMap()

    fun get(label: String): VerifiedPlantInfo? = entriesByLabel[label]

    fun metadataForLabel(label: String): Map<String, String> {
        val info = get(label)
        return if (info == null) {
            mapOf(
                "description" to "No description available for this plant.",
                "sourceAttribution" to "Local TensorFlow Lite model"
            )
        } else {
            mapOf(
                "commonName" to info.commonName,
                "description" to info.description,
                "scientificName" to info.scientificName,
                "sourceAttribution" to info.sourceAttribution
            )
        }
    }
}
