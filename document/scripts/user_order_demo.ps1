param(
    [string]$PortalBaseUrl = "http://localhost:8085",
    [string]$Username = "test",
    [string]$Password = "123",
    [long]$ProductSkuId = 106,
    [long]$ProductId = 29,
    [long]$MemberReceiveAddressId = 4
)

$ErrorActionPreference = "Stop"

function Invoke-PortalApi {
    param(
        [string]$Method,
        [string]$Path,
        [hashtable]$Query = @{},
        $Body = $null,
        [hashtable]$Headers = @{}
    )

    $queryString = ($Query.GetEnumerator() | ForEach-Object { "{0}={1}" -f $_.Key, [uri]::EscapeDataString([string]$_.Value) }) -join "&"
    $url = if ($queryString) { "$PortalBaseUrl$Path`?$queryString" } else { "$PortalBaseUrl$Path" }

    $params = @{
        Method = $Method
        Uri = $url
        Headers = $Headers
    }
    if ($null -ne $Body) {
        $params.Body = ($Body | ConvertTo-Json -Depth 6)
        $params.ContentType = "application/json"
    }

    return Invoke-RestMethod @params
}

Write-Host "[1/5] Member login"
$login = Invoke-PortalApi -Method Post -Path "/sso/login" -Query @{
    username = $Username
    password = $Password
}
if ($login.code -ne 200) { throw "Login failed: $($login.message)" }
$authHeader = @{ Authorization = "$($login.data.tokenHead) $($login.data.token)" }
Write-Host "      OK"

Write-Host "[2/5] Add cart item"
$cartBody = @{
    price = 5499
    productId = $ProductId
    productName = "Apple iPhone 8 Plus"
    productSkuCode = "201808270029001"
    productSkuId = $ProductSkuId
    productSubTitle = "demo"
    quantity = 1
    productAttr = "Gold;32G"
}
Invoke-PortalApi -Method Post -Path "/cart/add" -Body $cartBody -Headers $authHeader | Out-Null
Write-Host "      OK"

Write-Host "[3/5] Generate order"
$cartList = Invoke-PortalApi -Method Get -Path "/cart/list/promotion" -Headers $authHeader
$cartIds = @($cartList.data | ForEach-Object { $_.id })
if ($cartIds.Count -eq 0) { throw "Cart is empty" }

$orderBody = @{
    cartIds = $cartIds
    couponId = $null
    memberReceiveAddressId = $MemberReceiveAddressId
    payType = 1
    useIntegration = 0
}
$orderResult = Invoke-PortalApi -Method Post -Path "/order/generateOrder" -Body $orderBody -Headers $authHeader
if ($orderResult.code -ne 200) { throw "Order failed: $($orderResult.message)" }
$orderId = $orderResult.data.order.id
Write-Host "      orderId=$orderId"

Write-Host "[4/5] Mock pay"
$pay = Invoke-PortalApi -Method Post -Path "/order/mock-pay/$orderId" -Query @{ payType = 1 } -Headers $authHeader
if ($pay.code -ne 200) { throw "Pay failed: $($pay.message)" }
Write-Host "      $($pay.message)"

Write-Host "[5/5] Order detail"
$detail = Invoke-PortalApi -Method Get -Path "/order/detail/$orderId" -Headers $authHeader
if ($detail.code -ne 200) { throw "Detail failed: $($detail.message)" }
Write-Host "      orderSn=$($detail.data.orderSn) status=$($detail.data.status) payAmount=$($detail.data.payAmount)"

Write-Host ""
Write-Host "User order demo completed."
