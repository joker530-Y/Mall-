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

Write-Host "1) 会员登录"
$login = Invoke-PortalApi -Method Post -Path "/sso/login" -Query @{
    username = $Username
    password = $Password
}
if ($login.code -ne 200) { throw "登录失败: $($login.message)" }
$authHeader = @{ Authorization = "$($login.data.tokenHead)$($login.data.token)" }
Write-Host "   登录成功"

Write-Host "2) 加入购物车"
$cartBody = @{
    price = 5499
    productId = $ProductId
    productName = "Apple iPhone 8 Plus"
    productSkuCode = "201808270029001"
    productSkuId = $ProductSkuId
    productSubTitle = "demo"
    quantity = 1
    productAttr = "金色;32G"
}
Invoke-PortalApi -Method Post -Path "/cart/add" -Body $cartBody -Headers $authHeader | Out-Null
Write-Host "   加购成功"

Write-Host "3) 获取购物车并生成订单"
$cartList = Invoke-PortalApi -Method Get -Path "/cart/list/promotion" -Headers $authHeader
$cartIds = @($cartList.data | ForEach-Object { $_.id })
if ($cartIds.Count -eq 0) { throw "购物车为空" }

$orderBody = @{
    cartIds = $cartIds
    couponId = $null
    memberReceiveAddressId = $MemberReceiveAddressId
    payType = 1
    useIntegration = 0
}
$orderResult = Invoke-PortalApi -Method Post -Path "/order/generateOrder" -Body $orderBody -Headers $authHeader
if ($orderResult.code -ne 200) { throw "下单失败: $($orderResult.message)" }
$orderId = $orderResult.data.order.id
Write-Host "   下单成功, orderId=$orderId"

Write-Host "4) 模拟支付"
$pay = Invoke-PortalApi -Method Post -Path "/order/mock-pay/$orderId" -Query @{ payType = 1 } -Headers $authHeader
if ($pay.code -ne 200) { throw "支付失败: $($pay.message)" }
Write-Host "   $($pay.message)"

Write-Host "5) 查询订单详情"
$detail = Invoke-PortalApi -Method Get -Path "/order/detail/$orderId" -Headers $authHeader
if ($detail.code -ne 200) { throw "查询订单失败: $($detail.message)" }
Write-Host "   订单号=$($detail.data.orderSn), 状态=$($detail.data.status), 应付金额=$($detail.data.payAmount)"

Write-Host ""
Write-Host "用户端模拟下单链路演示完成。"
