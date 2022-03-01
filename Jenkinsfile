@Library('jenkins-shared-library')_
def helper = new helpers.PipelineHelper(this);

node (label: 'build_nodes') {
  def secrets = [
    [envVariable: 'AZURE_TMP_STORAGE_CONNECTION_STRING', name: 'azureTmpStorageConnectionString', secretType:'Secret']
  ]

  def URL = "reach-upload-service"
  def RESOURCE = "SNDCHMINFRGP001-${URL}-${helper.getEnvSuffix()}"
  def AI = "SNDCHMINFRGP001-${helper.getEnvSuffix()}"
  def APP = "${URL}-${helper.getEnvSuffix()}"
  def AZURE_TMP_STORAGE_CONTAINER_NAME = "reach-tmp-${helper.getEnvSuffix()}"

  withAzureKeyvault(secrets) {
    def envArray = [
      "APP_NAME=${APP}",
      "SERVICE_NAME=REACH Upload Service",
      "URL_PATH=${URL}",
      "RESOURCE_GROUP=${RESOURCE}",
      "BACKEND_PLAN=SNDCHMINFRGP001-${URL}-${helper.getEnvSuffix()}-service-plan",
      "AI_NAME=${AI}",
      "ACR_REPO=reach-upload-service/reach-upload-service",
      "SET_APP_LOGGING=false",
      "RUN_SONAR=true",
      "PROJECT_REPO_URL=https://giteux.azure.defra.cloud/chemicals/reach-upload-service.git",
      "CONNECTION_STRING=HTTP_UPLOAD_SERVICE_PLATFORM_PORT=8092 WEBSITES_PORT=8092 JWT_SECRET_KEY='MySecretKey' AZURE_TMP_STORAGE_CONTAINER_NAME='${AZURE_TMP_STORAGE_CONTAINER_NAME}' AZURE_TMP_STORAGE_CONNECTION_STRING='${AZURE_TMP_STORAGE_CONNECTION_STRING}' REACH_MONITORING_URL='https://reach-monitoring-dev.azurewebsites.net' AUDIT_API='https://reach-audit-dev.azurewebsites.net' USE_STUB_AV=true"
    ]

    withEnv(envArray) {
      def CREATE_DB = []
      def STORAGE_CONTAINERS = [AZURE_TMP_STORAGE_CONTAINER_NAME]
      def runIntegrationTests = {
        withMaven(
                options: [artifactsPublisher(disabled: true), jacocoPublisher(disabled: true)], mavenOpts: helper.getMavenOpts()
        ) {
          sh(label: "Run e2e tests", script: "mvn verify -P e2e-tests -DUPLOAD_API=https://${APP_NAME}.${APPLICATION_URL_SUFFIX}/")
        }
      }

      reachPipeline(CREATE_DB, STORAGE_CONTAINERS, runIntegrationTests)
    }

  }
}
