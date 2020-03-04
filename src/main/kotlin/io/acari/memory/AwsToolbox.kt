package io.acari.memory

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.net.URI

const val BUCKET_NAME = "sogos-avatars"

fun createS3Client(): S3Client {
  // todo: when not local no need
  val build = S3Client.builder()
    .region(Region.US_EAST_1)
    .endpointOverride(
      URI("http://172.17.0.1:5002")
    )
    .credentialsProvider {
      create(
        "key",
        "secret"
      )
    }
    .build()

  try {
    build.createBucket(
      CreateBucketRequest.builder()
        .bucket(BUCKET_NAME)
        .build()
    )
  } catch (e: Exception) {
    if (e !is BucketAlreadyExistsException) {
      System.err.println("Unable to create bucket for reasons")
      e.printStackTrace()
    }
  }

  return build
}

fun createS3Presigner(): S3Presigner =
  S3Presigner.builder().region(Region.US_WEST_2)
    .endpointOverride(URI("http://172.17.0.1:5002"))
    .credentialsProvider {
      create("access_key_id", "secret_key_id")
    }.build()
