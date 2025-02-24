# DME Product Catalog API

This API provides access to a DME (Durable Medical Equipment) product catalog stored in a GraphDB instance.

## Setup

1. Setup GraphDB(Container or a VM) and create a repository named `DME-Product-Catalog`
2. Load the sample data from `sample-data.ttl`
3. Create `local.settings.json` with the following configuration:
   ```json
   {
     "IsEncrypted": false,
     "Values": {
       "AzureWebJobsStorage": "UseDevelopmentStorage=true",
       "JAVA_VERSION": "21",
       "FUNCTIONS_WORKER_RUNTIME": "java",
       "AZURE_SUBSCRIPTION_ID": "your-subscription-id",
       "AZURE_RESOURCE_GROUP": "your-resource-group",
       "AZURE_PRICING_TIER": "Consumption",
       "AZURE_REGION": "centralus",
       "AZURE_RUNTIME_OS": "linux",
       "GRAPHDB_URL": "http://localhost:7200/repositories/DME-Product-Catalog"
     }
   }
    ```
These settings are used by both the Azure Functions runtime and the Gradle build script.

## Environment Variables

For deployment, ensure these environment variables are set:

- `AZURE_SUBSCRIPTION_ID`: Your Azure subscription ID
- `AZURE_RESOURCE_GROUP`: The Azure resource group to deploy to
- `AZURE_PRICING_TIER`: The Azure pricing tier (Consumption, Premium, etc.)
- `AZURE_REGION`: The Azure region to deploy to
- `AZURE_RUNTIME_OS`: The Azure runtime OS (Linux)
- `GRAPHDB_URL`: The URL of the GraphDB repository

## Sample Data

The sample data is available in `sample-data.ttl`. It includes:
- Products (Wheelchair, Oxygen Tank)
- Suppliers
- HCPCS Codes
- DX Codes

## API Usage

### Query by Product ID

``` GET /api/products?productId=1234 ```

### Query by HCPCS Code

``` GET /api/products?hcpcsCode=K0001 ```

### Query by DX Code

``` GET /api/products?dx=M17.11 ```

## Response Format

```json
[
  {
    "productName": "Wheelchair Model X",
    "price": "350.00",
    "supplierName": "ABC Medical Supplies",
    "hcpcsCode": "K0001",
    "dxCode": "M17.11"
  }
]