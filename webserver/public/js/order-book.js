/*
 * OrderBook
 * 
 * Provides a real-time update order book based on an existing HTML order book table on the page.
 */

/*
 * Constructor
 * url         URL for ajax call for server-side data
 * parameter   parameter for ajax call
 * symbol      the symbol for which this order book is created
 * clientData  data currently on client-side
 * targetRows  the rows of the target order book table
 * sorter      a sorter function that sorts orders strictly (order * order -> bool)
 * flashTime   the time for which new/removed rows should flash for (ms)
 */
function OrderBook(url, parameter, symbol, clientData, targetRows, sorter, flashTime) {
	this.url = url;
	this.parameter = parameter;
	this.symbol = symbol;
	this.clientData = clientData;
	this.targetRows = targetRows;
	this.sorter = sorter;
	this.flashTime = flashTime
}

/*
 * Refresh the order book data via an AJAX poll
 * (currently polls the server every second)
 */
OrderBook.prototype.refresh = function() {
	console.log("called");
	var self = this;
	$.ajax({
		url : this.url + this.parameter,
		cache: false,
		dataType : 'json',
		success : function(result) {
			console.log("sucesss");
			if(result) {
				// Update the order book view
				self.update(result.data, self.clientData, self.targetRows, self.sorter);
				// Update client-side data
				self.clientData = result.data;
			}
			console.log("setting");
			window.setInterval(self.refresh, 1000);
		}
	});
}

/*
 * Update the HTML table view of the order book
 * serverData  data for table view retrieved from the server
 * clientData  data currently representing the table view on the client
 * tableRows   the target set of rows of the table
 * sorter      the function to sort order book entries
 */
OrderBook.prototype.update = function(serverData, clientData, tableRows, sorter) {
	var self = this;
	
	// Construct lists of entries to insert or remove
	var insertList = this.listSubtract(serverData, clientData, this.compareOrders);
	var removeList = this.listSubtract(clientData, serverData, this.compareOrders);
	
	// Reduce the remove list to an array of order IDs
	removeList = this.extractArrayOrderID(removeList);

	var rowCount = 25;
	// Loop over all rows in the HTML target table
	tableRows.each(function(index, row) {
		row = $(row);
		// Insert rows before the current row
		$.each(insertList, function(i, insertOrder) {
			if (insertOrder) {
				if (sorter(row.data('price'), insertOrder.price)) {
					self.insertRowBefore(row, insertOrder);
					insertList[i] = null; // Null the order once it has been inserted
				} else {
					if (index < rowCount - 2) {
						return false;
					} else {
						// If we reach the last item in the list, insert all of the remaining
						// orders to be inserted after the last item
						self.insertRowAfter(row, insertOrder);
						insertList[i] = null;
					}
				}
			}
		});
		// Remove row if required
		var orderID = row.data('order-id');
		if ($.inArray(orderID, removeList) >= 0) {
			self.flashElement(row, function() {
				row.remove();
			});
		}
	});
}

/*
 * Compare if two orders are the same
 * This is a comparator function which currently defines equality as having the same order ID.
 * orderA  first order to compare
 * orderB  second order to compare
 */
OrderBook.prototype.compareOrders = function(orderA, orderB) {
	return (orderA.order_id == orderB.order_id);
}

/*
 * Subtract one list of objects from another using a comparator for object comparison
 * (result = listA - listB)
 * listA       list of objects to subtract from
 * listB       list of objects to subtract
 * comparator  comparison function for objects in listA and listB
 */
OrderBook.prototype.listSubtract = function(listA, listB, comparator) {
	var found = false;
	var resultList = [];
	$.each(listA, function(indexA, elementA) {
		$.each(listB, function(indexB, elementB) {
			if (comparator(elementA, elementB)) {
				found = true;
				return false;
			}
		});
		if (!found) {
			resultList.push(elementA);
		}
		found = false;
	});
	return resultList;
}

/*
 * Extract an array of order IDs from an array of orders
 * orderArray  array of orders
 */
OrderBook.prototype.extractArrayOrderID = function(orderArray) {
	var result = [];
	$.each(orderArray, function(index, order) {
		result.push(parseInt(order.order_id));
	});
	return result;
}

/*
 * Flash a row in the table
 * element   the element to flash
 * callback  callback function
 */
OrderBook.prototype.flashElement = function(element, callback) {
	var self = this;
	element.addClass('flash-old');
	var interval = window.setTimoue(
		function() { callback(); },
		self.flashTime
	);
}

/*
 * Generate an HTML table row from an order
 * order  an order book order
 */
OrderBook.prototype.generateRow = function(order) {
	return '<tr class="flash-new" data-order-id="'+order.order_id+'" data-price="'+order.price+'">'
	   +'<td>'+(order.price * this.symbol.price_scale).toFixed(2)+'</td>'
	   +'<td>'+order.volume+'</td>'
	   +'<td>'+(order.price * this.symbol.price_scale * order.volume).toFixed(2)+'</td>'
	   +'<td>'+moment(order.updated_s * 1000).format('DD/MM/YYYY - HH:mm:ss')+'</td>'
	   +'</tr>';
}
/*
 * Insert a row before another row in the table (also momentarily flash the row)
 * row    the row to insert the new row before
 * order  the order object for the new row
 */
OrderBook.prototype.insertRowBefore = function(row, order) {
	var self = this;
	var htmlRow = this.generateRow(order);
	var newRow = $(htmlRow).insertBefore(row);
	var interval = window.setTimeout(
		function() { newRow.removeClass('flash-new'); },
		self.flashTime
	);
}

OrderBook.prototype.insertRowAfter = function(row, order) {
	var self = this;
	var htmlRow = this.generateRow(order);
	var newRow = $(htmlRow).insertAfter(row);
	var interval = window.setTimeout(
		function() { newRow.removeClass('flash-new'); },
		self.flashTime
	);
}

/*
 * Document ready function
 */
$(document).ready(function() {

	var offerTable = new OrderBook(
		'/api/v1/orders/offers/',
		client.symbol.symbol_id,
		client.symbol,
		client.offerList,
		$('#offer-rows tr'),
		function(orderA, orderB) { return (orderA >= orderB); },
		800
	);
	offerTable.refresh();
	//window.setInterval(offerTable.refresh, 1000);
	
	var bidTable = new OrderBook(
		'/api/v1/orders/bids/',
		client.symbol.symbol_id,
		client.symbol,
		client.bidList,
		$('#bid-rows tr'),
		function(orderA, orderB) { return (orderA <= orderB); },
		800
	);
	bidTable.refresh();
	//window.setInterval(bidTable.refresh, 1000);
});