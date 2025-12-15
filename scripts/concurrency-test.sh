#!/bin/bash

# =============================================================================
# Concurrency Test Script for 85labs E-commerce
# Tests optimistic locking behavior when multiple requests compete for stock
# =============================================================================

BASE_URL="${BASE_URL:-http://localhost:8080}"
API_URL="$BASE_URL/api/v1"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=============================================${NC}"
echo -e "${BLUE}   85labs E-commerce Concurrency Test${NC}"
echo -e "${BLUE}=============================================${NC}"
echo ""

# Step 1: Register a test user and get JWT token
echo -e "${YELLOW}[1/5] Registering test user...${NC}"
REGISTER_RESPONSE=$(curl -s -X POST "$API_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "concurrency-test-'$(date +%s)'@test.com",
    "password": "test123456",
    "name": "Concurrency Tester"
  }')

TOKEN=$(echo $REGISTER_RESPONSE | jq -r '.data.token // empty')

if [ -z "$TOKEN" ]; then
  echo -e "${YELLOW}User might exist, trying login...${NC}"
  LOGIN_RESPONSE=$(curl -s -X POST "$API_URL/auth/login" \
    -H "Content-Type: application/json" \
    -d '{
      "email": "concurrency-test@test.com",
      "password": "test123456"
    }')
  TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.data.token // empty')
fi

if [ -z "$TOKEN" ]; then
  echo -e "${RED}Failed to get authentication token${NC}"
  exit 1
fi

echo -e "${GREEN}Got JWT token${NC}"

# Step 2: Create a category (if needed)
echo -e "${YELLOW}[2/5] Creating test category...${NC}"
CATEGORY_RESPONSE=$(curl -s -X POST "$API_URL/categories" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "Test Category",
    "description": "Category for concurrency testing"
  }')

CATEGORY_ID=$(echo $CATEGORY_RESPONSE | jq -r '.data.id // empty')
echo -e "${GREEN}Category ID: $CATEGORY_ID${NC}"

# Step 3: Create a product with LIMITED STOCK (only 1 unit!)
echo -e "${YELLOW}[3/5] Creating product with stock = 1...${NC}"
PRODUCT_CODE="CONCURRENCY-TEST-$(date +%s)"
PRODUCT_RESPONSE=$(curl -s -X POST "$API_URL/products" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "Limited Edition Item",
    "description": "Only 1 in stock - for concurrency testing",
    "code": "'$PRODUCT_CODE'",
    "price": 99.99,
    "stock": 1,
    "categoryId": "'$CATEGORY_ID'"
  }')

PRODUCT_ID=$(echo $PRODUCT_RESPONSE | jq -r '.data.id // empty')
INITIAL_STOCK=$(echo $PRODUCT_RESPONSE | jq -r '.data.stock // empty')

if [ -z "$PRODUCT_ID" ]; then
  echo -e "${RED}Failed to create product${NC}"
  echo $PRODUCT_RESPONSE | jq .
  exit 1
fi

echo -e "${GREEN}Product ID: $PRODUCT_ID${NC}"
echo -e "${GREEN}Initial Stock: $INITIAL_STOCK${NC}"

# Step 4: Launch concurrent order requests
echo ""
echo -e "${YELLOW}[4/5] Launching 10 concurrent order requests...${NC}"
echo -e "${YELLOW}      All trying to buy the SAME product with only 1 unit in stock${NC}"
echo ""

# Create a temporary file to store results
RESULTS_FILE=$(mktemp)

# Function to place an order
place_order() {
  local request_num=$1
  local response=$(curl -s -w "\n%{http_code}" -X POST "$API_URL/orders" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TOKEN" \
    -d '{
      "items": [
        {
          "productId": "'$PRODUCT_ID'",
          "quantity": 1
        }
      ]
    }')

  local http_code=$(echo "$response" | tail -n1)
  local body=$(echo "$response" | sed '$d')

  echo "$request_num|$http_code|$body" >> $RESULTS_FILE
}

# Launch 10 concurrent requests
for i in {1..10}; do
  place_order $i &
done

# Wait for all requests to complete
wait

echo -e "${BLUE}All requests completed!${NC}"
echo ""

# Step 5: Analyze results
echo -e "${YELLOW}[5/5] Analyzing results...${NC}"
echo ""

SUCCESS_COUNT=0
CONFLICT_COUNT=0
ERROR_COUNT=0

while IFS='|' read -r request_num http_code body; do
  if [ "$http_code" == "201" ] || [ "$http_code" == "200" ]; then
    echo -e "${GREEN}Request #$request_num: SUCCESS (HTTP $http_code) - Order created!${NC}"
    SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
  elif [ "$http_code" == "409" ]; then
    echo -e "${YELLOW}Request #$request_num: CONFLICT (HTTP $http_code) - Insufficient stock${NC}"
    CONFLICT_COUNT=$((CONFLICT_COUNT + 1))
  else
    echo -e "${RED}Request #$request_num: ERROR (HTTP $http_code)${NC}"
    ERROR_COUNT=$((ERROR_COUNT + 1))
  fi
done < $RESULTS_FILE

# Cleanup
rm -f $RESULTS_FILE

echo ""
echo -e "${BLUE}=============================================${NC}"
echo -e "${BLUE}                 SUMMARY${NC}"
echo -e "${BLUE}=============================================${NC}"
echo -e "${GREEN}Successful orders: $SUCCESS_COUNT${NC}"
echo -e "${YELLOW}Rejected (no stock): $CONFLICT_COUNT${NC}"
echo -e "${RED}Errors: $ERROR_COUNT${NC}"
echo ""

# Verify final stock
echo -e "${YELLOW}Verifying final product stock...${NC}"
FINAL_PRODUCT=$(curl -s -X GET "$API_URL/products/$PRODUCT_ID" \
  -H "Authorization: Bearer $TOKEN")

FINAL_STOCK=$(echo $FINAL_PRODUCT | jq -r '.data.stock // empty')
FINAL_VERSION=$(echo $FINAL_PRODUCT | jq -r '.data.version // empty')

echo -e "${BLUE}Final Stock: $FINAL_STOCK${NC}"
echo -e "${BLUE}Final Version: $FINAL_VERSION${NC}"
echo ""

# Validate results
if [ "$SUCCESS_COUNT" -eq 1 ] && [ "$FINAL_STOCK" -eq 0 ]; then
  echo -e "${GREEN}=============================================${NC}"
  echo -e "${GREEN}  TEST PASSED! Optimistic locking works!${NC}"
  echo -e "${GREEN}=============================================${NC}"
  echo -e "${GREEN}Only 1 order succeeded out of 10 concurrent requests.${NC}"
  echo -e "${GREEN}Stock was correctly decremented from 1 to 0.${NC}"
  echo -e "${GREEN}No overselling occurred!${NC}"
else
  echo -e "${RED}=============================================${NC}"
  echo -e "${RED}  TEST RESULTS UNEXPECTED${NC}"
  echo -e "${RED}=============================================${NC}"
  echo -e "${RED}Expected: 1 success, 9 conflicts${NC}"
  echo -e "${RED}Got: $SUCCESS_COUNT successes, $CONFLICT_COUNT conflicts${NC}"
fi

echo ""
echo "Done!"
