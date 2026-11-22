idf_component_register(
    SRCS "main.c" "ble_cuktech.c" "cuktech.c" "mi_crypto.c" "miot.c"
    INCLUDE_DIRS "."
    REQUIRES bt mqtt json mbedtls esp_wifi nvs_flash esp_timer esp_event esp_hw_support
)
