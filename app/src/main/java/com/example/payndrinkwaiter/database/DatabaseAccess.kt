package com.example.payndrinkwaiter.database

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

//waiter model class
data class Waiter(val id: Int, val firstName: String?, val lastName: String?, val description: String?,
                  val pictureUrl: String?, val restaurantID: Int)

//order model class
data class Order(val id: Int, val price: Double, val placed: Long?, val fulfilled: Long?,
                 val accepted: Long?, val rejected: Long?, val refund: Double?,
                 val refundReason: String?, val seat: Int, val waiterID: Int?)

//order has items model class
data class OrderHasItems(val id: Int, val quantity: Int, val delivered: Int, val refunded: Int,
                         val itemID: Int, val itemName: String?, val orderID: Int)

class DatabaseAccess {

    fun connectToDatabase(): Connection? {
        val jdbcUrl =
            "jdbc:postgresql://dpg-cdj2l8mn6mpngrtb5a5g-a.frankfurt-postgres.render.com/" +
                    "tvt21kmo_r3_mobiiliprojekti"
        val userName = "tvt21kmo_r3"
        val password = "H9V1M6gtYtQmWg6nJiqU0sstkCs2LxTl"
        var connection: Connection? = null
        try{
            connection = DriverManager.getConnection(jdbcUrl, userName, password)
        }catch(e: SQLException){
            println(e.toString())
        }
        return connection
    }

    fun getItemsInOrder(connection: Connection, orderID: Int): MutableList<OrderHasItems>{
        val query = "SELECT * FROM order_has_item WHERE id_order=$orderID"
        val result = connection.prepareStatement(query).executeQuery()
        val items = mutableListOf<OrderHasItems>()
        while(result.next()){
            val id = result.getInt("id_order_has_item")
            val quantity = result.getInt("quantity")
            val delivered = result.getInt("delivered")
            val refunded = result.getInt("refunded")
            val itemID = result.getInt("id_item")
            val itemName = getItemNameByID(connection, itemID)
            items.add(OrderHasItems(id, quantity, delivered, refunded, itemID, itemName, orderID))
        }
        return items
    }

    private fun getItemNameByID(connection: Connection, itemID: Int): String?{
        val query = "SELECT item_name FROM item WHERE id_item=$itemID"
        val result = connection.prepareStatement(query).executeQuery()
        var name: String? = null
        while(result.next()){
            name = result.getString("item_name")
        }
        return name
    }

    private fun getSeatByID(connection: Connection, seatID:Int): Int{
        val query = "SELECT seat_number FROM seating WHERE id_seating=$seatID"
        val result = connection.prepareStatement(query).executeQuery()
        var seat = 0
        while(result.next()){
            seat = result.getInt("seat_number")
        }
        return seat
    }

    fun getNewOrders(connection: Connection): MutableList<Order>{
        val query = "SELECT * FROM orders WHERE order_fulfilled IS NULL AND order_placed IS NOT NULL"
        val result = connection.prepareStatement(query).executeQuery()
        val orders = mutableListOf<Order>()
        while(result.next()){
            val id = result.getInt("id_order")
            val price = result.getDouble("order_price")
            val placed = result.getLong("order_placed")
            val accepted = result.getLong("order_accepted")
            val rejected = result.getLong("order_rejected")
            val seat = getSeatByID(connection, result.getInt("id_seating"))
            val waiter = result.getInt("id_waiter")
            orders.add(Order(id, price, placed, null, accepted, rejected, null,
                null, seat, waiter))
        }
        return orders
    }

    fun getWaiterByID(connection: Connection, waiterID: Int): Waiter?{
        val query = "SELECT * FROM waiter WHERE id_waiter=$waiterID"
        val result = connection.prepareStatement(query).executeQuery()
        var waiter: Waiter? = null
        while (result.next()){
            val firstName = result.getString("first_name")
            val lastName = result.getString("last_name")
            val pictureUrl = result.getString("picture_url")
            val description = result.getString("waiter_description")
            val restaurantID = result.getInt("id_restaurant")
            waiter = Waiter(waiterID, firstName, lastName, description, pictureUrl, restaurantID)
        }
        return waiter
    }

    fun getItemPrice(connection: Connection, itemID: Int): Double{
        var price = 0.00
        val query = "SELECT price FROM item WHERE id_item=$itemID"
        val result = connection.prepareStatement(query).executeQuery()
        while(result.next()){
            price = result.getDouble("price")
        }
        return price
    }

    fun setItemDelivered(connection: Connection, idOrderItem: Int, delivered: Int): Int{
        var id = 0
        val query = "UPDATE order_has_item SET delivered=delivered+$delivered WHERE " +
                "id_order_has_item=$idOrderItem RETURNING id_item"
        val result = connection.prepareStatement(query).executeQuery()
        while(result.next()){
            id = result.getInt("id_item")
        }
        return id
    }

    fun setItemRefunded(connection: Connection, idOrderItem: Int, refunded: Int): Int{
        var id = 0
        val query = "UPDATE order_has_item SET refunded=refunded+$refunded WHERE " +
                "id_order_has_item=$idOrderItem RETURNING id_item"
        val result = connection.prepareStatement(query).executeQuery()
        while(result.next()){
            id = result.getInt("id_item")
        }
        return id
    }
    fun acceptOrder(connection: Connection, orderID: Int, waiterID: Int, timeToDeliver: Int): Int{
        var id = 0
        val acceptTime = System.currentTimeMillis()
        val exceptedDelivery = acceptTime + timeToDeliver * 60000
        val query = "UPDATE orders SET id_waiter=$waiterID,order_accepted=$acceptTime," +
                "expected_delivery=$exceptedDelivery WHERE id_order=$orderID RETURNING id_seating"
        val result = connection.prepareStatement(query).executeQuery()
        while(result.next()){
            id = result.getInt("id_seating")
        }
        return id
    }

    fun rejectOrder(connection: Connection, orderID: Int, waiterID: Int, reason: String): Int{
        var id = 0
        val rejectTime = System.currentTimeMillis()
        val query = "UPDATE orders SET id_waiter=$waiterID,order_rejected=$rejectTime," +
                "reject_reason='$reason' WHERE id_order=$orderID RETURNING id_seating"
        val result = connection.prepareStatement(query).executeQuery()
        while(result.next()){
            id = result.getInt("id_seating")
        }
        return id
    }

    fun fulfillOrder(connection: Connection, orderID: Int): Int{
        var id = 0
        val orderTime = System.currentTimeMillis()
        val query = "UPDATE orders SET order_fulfilled=$orderTime WHERE id_order=$orderID RETURNING id_seating"
        val result = connection.prepareStatement(query).executeQuery()
        while(result.next()){
            id = result.getInt("id_seating")
        }
        return id
    }

    fun refundOrder(connection: Connection, orderID: Int, refund: Double, refundReason: String): Int{
        var id = 0
        val query = "UPDATE orders SET refund=$refund,refund_reason='$refundReason' " +
                "WHERE id_order=$orderID RETURNING id_seating"
        val result = connection.prepareStatement(query).executeQuery()
        while(result.next()){
            id = result.getInt("id_seating")
        }
        return id
    }
}