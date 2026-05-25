import re

with open("src/main/kotlin/com/crust/menu/graphql/MenuGraphMutations.kt", "r") as f:
    mut_content = f.read()

# Replace eightySixItem
mut_content = mut_content.replace('fun eightySixItem(@InputArgument itemId: String): Map<String, Any>', 'fun eightySixItem(@InputArgument itemId: String): com.crust.menu.domain.MenuItem')
mut_content = mut_content.replace('fun unEightySixItem(@InputArgument itemId: String, @InputArgument quantity: Int?): Map<String, Any>', 'fun unEightySixItem(@InputArgument itemId: String, @InputArgument quantity: Int?): com.crust.menu.domain.MenuItem')

# Replace createOrder
create_order_old = """    @DgsMutation
    fun createOrder(@InputArgument input: Map<String, Any>): Map<String, Any> {
        val items = (input["items"] as List<Map<String, Any>>).map { item ->
            val modifiers = (item["modifiers"] as? List<Map<String, Any>>)?.map { m ->
                ModifierSelectionInput(
                    modifierId = m["modifierId"] as? String,
                    modifierName = m["modifierName"] as String,
                    priceImpact = (m["priceImpact"] as? Number)?.toDouble() ?: 0.0
                )
            }

            CreateOrderItemInput(
                menuItemId = item["menuItemId"] as String,
                quantity = (item["quantity"] as? Number)?.toInt() ?: 1,
                modifierSelections = item["modifierSelections"] as? String,
                modifiers = modifiers,
                specialInstructions = item["specialInstructions"] as? String
            )
        }

        val orderInput = CreateOrderInput(
            channel = input["channel"] as String,
            restaurantId = input["restaurantId"] as? String,
            locationId = input["locationId"] as? String,
            tableNumber = input["tableNumber"] as? String,
            serverName = input["serverName"] as? String,
            guestCount = (input["guestCount"] as? Number)?.toInt(),
            idempotencyKey = input["idempotencyKey"] as? String,
            notes = input["notes"] as? String,
            items = items
        )

        val order = orderService.createOrder(orderInput)
        return orderToMap(order)
    }"""
create_order_new = """    @DgsMutation
    fun createOrder(@InputArgument("input") input: CreateOrderInput): com.crust.menu.domain.RestaurantOrder {
        return orderService.createOrder(input)
    }"""
mut_content = mut_content.replace(create_order_old, create_order_new)

# Replace updateOrderStatus
mut_content = mut_content.replace('fun updateOrderStatus(@InputArgument orderId: String, @InputArgument status: String): Map<String, Any> {', 'fun updateOrderStatus(@InputArgument orderId: String, @InputArgument status: String): com.crust.menu.domain.RestaurantOrder {')
mut_content = mut_content.replace('return orderToMap(order)', 'return order')

# Replace KDS
mut_content = mut_content.replace('fun acknowledgeKitchenTicket(@InputArgument ticketId: String): Map<String, Any> {', 'fun acknowledgeKitchenTicket(@InputArgument ticketId: String): com.crust.menu.domain.KitchenTicket {')
mut_content = mut_content.replace('return ticketToMap(ticket)', 'return ticket')

mut_content = mut_content.replace('fun markKitchenTicketReady(@InputArgument ticketId: String): Map<String, Any> {', 'fun markKitchenTicketReady(@InputArgument ticketId: String): com.crust.menu.domain.KitchenTicket {')

mut_content = mut_content.replace('fun bumpKitchenTicket(@InputArgument ticketId: String): Map<String, Any> {', 'fun bumpKitchenTicket(@InputArgument ticketId: String): com.crust.menu.domain.KitchenTicket {')

# Replace Payments
pay_old = """    @DgsMutation
    fun processPayment(
        @InputArgument orderId: String,
        @InputArgument amount: Double,
        @InputArgument tipAmount: Double?,
        @InputArgument method: String
    ): Map<String, Any> {
        val payment = paymentService.processPayment(
            orderId = UUID.fromString(orderId),
            amount = BigDecimal(amount),
            tipAmount = tipAmount?.let { BigDecimal(it) } ?: BigDecimal.ZERO,
            method = method
        )
        return paymentToMap(payment)
    }"""
pay_new = """    @DgsMutation
    fun processPayment(
        @InputArgument orderId: String,
        @InputArgument amount: Double,
        @InputArgument tipAmount: Double?,
        @InputArgument method: String
    ): com.crust.menu.domain.Payment {
        return paymentService.processPayment(
            orderId = UUID.fromString(orderId),
            amount = BigDecimal(amount),
            tipAmount = tipAmount?.let { BigDecimal(it) } ?: BigDecimal.ZERO,
            method = method
        )
    }"""
mut_content = mut_content.replace(pay_old, pay_new)

with open("src/main/kotlin/com/crust/menu/graphql/MenuGraphMutations.kt", "w") as f:
    f.write(mut_content)


# Now fix Queries
with open("src/main/kotlin/com/crust/menu/graphql/MenuGraphQueries.kt", "r") as f:
    q_content = f.read()

# getActiveMenu
q_content = q_content.replace('fun getActiveMenu(): Map<String, Any>? {', 'fun getActiveMenu(): com.crust.menu.domain.MenuVersion? {')
q_content = q_content.replace('return menuToMap(activeMenu)', 'return activeMenu')

# getOrder
q_content = q_content.replace('fun getOrder(@InputArgument id: String): Map<String, Any>? {', 'fun getOrder(@InputArgument id: String): com.crust.menu.domain.RestaurantOrder? {')
q_content = q_content.replace('return orderToMap(order)', 'return order')

# getActiveOrders
q_content = q_content.replace('fun getActiveOrders(): List<Map<String, Any>> {', 'fun getActiveOrders(): List<com.crust.menu.domain.RestaurantOrder> {')
q_content = q_content.replace('return orderService.getActiveOrders().map { orderToMap(it) }', 'return orderService.getActiveOrders()')

# getOrdersByStatus
q_content = q_content.replace('fun getOrdersByStatus(@InputArgument status: String): List<Map<String, Any>> {', 'fun getOrdersByStatus(@InputArgument status: String): List<com.crust.menu.domain.RestaurantOrder> {')
q_content = q_content.replace('return orderService.getOrdersByStatus(status).map { orderToMap(it) }', 'return orderService.getOrdersByStatus(status)')

# getKitchenStations
q_content = q_content.replace('fun getKitchenStations(): List<Map<String, Any>> {', 'fun getKitchenStations(): List<com.crust.menu.domain.KitchenStation> {')
q_content = q_content.replace('return kitchenDisplayService.getStations().map { mapOf("id" to it.id.toString(), "name" to it.name, "isActive" to it.isActive) }', 'return kitchenDisplayService.getStations()')

# getKitchenTickets
q_content = q_content.replace('fun getKitchenTickets(@InputArgument stationId: String?): List<Map<String, Any>> {', 'fun getKitchenTickets(@InputArgument stationId: String?): List<com.crust.menu.domain.KitchenTicket> {')
q_content = q_content.replace('return tickets.map { ticketToMap(it) }', 'return tickets')

# getPaymentsForOrder
q_content = q_content.replace('fun getPaymentsForOrder(@InputArgument orderId: String): List<Map<String, Any>> {', 'fun getPaymentsForOrder(@InputArgument orderId: String): List<com.crust.menu.domain.Payment> {')
q_content = q_content.replace('return paymentService.getPaymentsForOrder(UUID.fromString(orderId)).map { paymentToMap(it) }', 'return paymentService.getPaymentsForOrder(UUID.fromString(orderId))')

# itemSalesHourly
q_content = q_content.replace('fun itemSalesHourly(@InputArgument dateFrom: String, @InputArgument dateTo: String): List<Map<String, Any>> {', 'fun itemSalesHourly(@InputArgument dateFrom: String, @InputArgument dateTo: String): List<com.crust.menu.domain.ItemSalesHourly> {')
q_content = q_content.replace('return reportingService.getItemSalesHourly(from, to).map { salesToMap(it) }', 'return reportingService.getItemSalesHourly(from, to)')

# demandForecast
q_content = q_content.replace('fun demandForecast(@InputArgument dateFrom: String, @InputArgument dateTo: String): List<Map<String, Any>> {', 'fun demandForecast(@InputArgument dateFrom: String, @InputArgument dateTo: String): List<com.crust.menu.domain.ItemDemandForecast> {')
q_content = q_content.replace('return demandForecastService.getForecast(from, to)\n            .map { forecastToMap(it) }', 'return demandForecastService.getForecast(from, to)')

# itemForecast
q_content = q_content.replace('fun itemForecast(@InputArgument menuItemId: String): List<Map<String, Any>> {', 'fun itemForecast(@InputArgument menuItemId: String): List<com.crust.menu.domain.ItemDemandForecast> {')
q_content = q_content.replace('return demandForecastService.getForecastForItem(UUID.fromString(menuItemId))\n            .map { forecastToMap(it) }', 'return demandForecastService.getForecastForItem(UUID.fromString(menuItemId))')

# predictive86Alerts
q_content = q_content.replace('fun predictive86Alerts(@InputArgument status: String?): List<Map<String, Any>> {', 'fun predictive86Alerts(@InputArgument status: String?): List<com.crust.menu.domain.Predictive86Alert> {')
q_content = q_content.replace('return alerts.map { alertToMap(it) }', 'return alerts')


with open("src/main/kotlin/com/crust/menu/graphql/MenuGraphQueries.kt", "w") as f:
    f.write(q_content)

