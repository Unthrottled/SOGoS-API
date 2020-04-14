package io.acari.memory

import AWS_ACCESS_KEY
import AWS_SECRET_ACCESS
import io.vertx.core.json.JsonObject
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.presigner.S3Presigner

const val BUCKET_NAME = "sogos-avatars"

fun createS3Presigner(configuration: JsonObject): S3Presigner =
  S3Presigner.builder()
    .region(Region.US_EAST_1)
    .credentialsProvider {
      create(configuration.getString(AWS_ACCESS_KEY), configuration.getString(AWS_SECRET_ACCESS))
    }.build()
