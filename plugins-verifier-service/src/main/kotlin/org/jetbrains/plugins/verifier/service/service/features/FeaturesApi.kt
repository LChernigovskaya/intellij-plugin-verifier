package org.jetbrains.plugins.verifier.service.service.features

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.jetbrains.intellij.feature.extractor.ExtensionPoint
import com.jetbrains.intellij.feature.extractor.ExtensionPointFeatures

/**
 * Converts the internal feature extractor [results] [ExtractFeaturesTask.Result]
 * to the protocol API version of [results] [ApiFeaturesResult].
 */
fun ExtractFeaturesTask.Result.prepareFeaturesResponse(): String {
  val updateId = updateInfo.updateId
  val apiResultType = resultType.convertResultType()
  val apiFeatures = convertFeatures(features)
  return Gson().toJson(ApiFeaturesResult(updateId, apiResultType, apiFeatures))
}

private fun convertFeatures(features: List<ExtensionPointFeatures>): List<ApiExtensionPointFeatures> = features.map {
  val apiExtensionPoint = convertExtensionPoint(it)
  ApiExtensionPointFeatures(apiExtensionPoint, it.epImplementorName, it.featureNames)
}

private fun convertExtensionPoint(it: ExtensionPointFeatures): ApiExtensionPoint = when (it.extensionPoint) {
  ExtensionPoint.CONFIGURATION_TYPE -> ApiExtensionPoint.CONFIGURATION_TYPE
  ExtensionPoint.FACET_TYPE -> ApiExtensionPoint.FACET_TYPE
  ExtensionPoint.FILE_TYPE -> ApiExtensionPoint.FILE_TYPE
  ExtensionPoint.ARTIFACT_TYPE -> ApiExtensionPoint.ARTIFACT_TYPE
  ExtensionPoint.MODULE_TYPE -> ApiExtensionPoint.MODULE_TYPE
}

private fun ExtractFeaturesTask.Result.ResultType.convertResultType(): ApiFeaturesResult.ResultType = when (this) {
  ExtractFeaturesTask.Result.ResultType.NOT_FOUND -> ApiFeaturesResult.ResultType.NOT_FOUND
  ExtractFeaturesTask.Result.ResultType.FAILED_TO_DOWNLOAD -> ApiFeaturesResult.ResultType.NOT_FOUND
  ExtractFeaturesTask.Result.ResultType.BAD_PLUGIN -> ApiFeaturesResult.ResultType.BAD_PLUGIN
  ExtractFeaturesTask.Result.ResultType.EXTRACTED_ALL -> ApiFeaturesResult.ResultType.EXTRACTED_ALL
  ExtractFeaturesTask.Result.ResultType.EXTRACTED_PARTIALLY -> ApiFeaturesResult.ResultType.EXTRACTED_PARTIALLY
}


private data class ApiExtensionPointFeatures(
    @SerializedName("extensionPoint") val extensionPoint: ApiExtensionPoint,
    @SerializedName("implementorName") val epImplementorName: String,
    @SerializedName("featureNames") val featureNames: List<String>
)

private data class ApiFeaturesResult(
    @SerializedName("updateId") val updateId: Int,
    @SerializedName("resultType") val resultType: ResultType,
    @SerializedName("features") val features: List<ApiExtensionPointFeatures> = emptyList()
) {
  enum class ResultType {
    NOT_FOUND,
    BAD_PLUGIN,
    EXTRACTED_ALL,
    EXTRACTED_PARTIALLY
  }
}

private enum class ApiExtensionPoint {
  CONFIGURATION_TYPE,
  FACET_TYPE,
  FILE_TYPE,
  ARTIFACT_TYPE,
  MODULE_TYPE
}