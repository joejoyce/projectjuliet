/*
 * OrderBook
 * 
 * Provides a real-time update order book based on an existing HTML order book table on the page.
 */

/*
 * Constructor
 * url               URL for ajax call for server-side data
 * parameter         parameter for ajax call
 * symbol            the symbol for which this order book is created
 * clientData        data currently on client-side
 * targetRows        the rows of the target order book table
 * sorter            a sorter function that sorts orders strictly (order * order -> bool)
 * insertFlashTime   the time for which newly inserted rows should flash (ms)
 * removeFlashTime   the time for which removed rows should flash (ms)
 */
function OrderBook(url, parameter, symbol, clientData, targetRows, sorter, insertFlashTime, removeFlashTime) {
	this.url = url;
	this.parameter = parameter;
	this.symbol = symbol;
	this.clientData = clientData;
	this.targetRows = targetRows;
	this.sorter = sorter;
	this.insertFlashTime = insertFlashTime;
	this.removeFlashTime = removeFlashTime;
}

/*
 * Refresh the order book data via an AJAX poll
 * (currently polls the server every second)
 */
OrderBook.prototype.refresh = function(orderBook) {
	var self = this;
	$.ajax({
		url : orderBook.url + orderBook.parameter,
		cache: false,
		dataType : 'json',
		success : function(result) {
			if(result) {
				// Update the order book view
				orderBook.update(result.data, orderBook.clientData, orderBook.targetRows, orderBook.sorter);
				// Update client-side data
				orderBook.clientData = result.data;
			}
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
	console.log("inlen: " + insertList.length + ", remlen: " + removeList.length);
	console.log("targetlength: " + self.targetRows.length);
        console.dir($(self.targetRows));
	// Reduce the remove list to an array of order IDs
	removeList = this.extractArrayOrderID(removeList);

	var rowCount = 25;
	// Perform required insertions
        insertList.forEach(function(insertOrder) {
		$(self.targetRows).each(function(index, row) {
			//console.log("why>?: " + index);
			row = $(row);
			if (sorter(parseInt(row.data('price')), insertOrder.price)) {
				self.insertRowBefore(row, insertOrder);
				return false;
			} else {
				if (index >= rowCount - 2) {
					self.insertRowAfter(row, insertOrder);
					return false;
				}
			}
		});
	});
	// Remove rows
	var duplicates = [];
	$(self.targetRows).each(function(index, row) {
		row = $(row);
		if ($.inArray(row.data('order-id'), duplicates) >= 0) {
			row.remove();
		} else {
			duplicates.push(row.data('order-id'));
			if ($.inArray(row.data('order-id'), removeList) >= 0) {
				self.flashElement(row, function() {
					$(row).find('td').wrapInner('<div style="display:block;" />')
  				.parent()
    			.find('td > div')
					.slideUp(700, function() {
						$(this).parent().parent().remove();
						row.remove();
					});
				});
			}
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

	var interval = window.setTimeout(
		function() { callback(); },
		self.removeFlashTime
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
	   +'<td>'+moment((order.updated_s-18000) * 1000).format('DD/MM/YYYY - HH:mm:ss')+'</td>'
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
	$(newRow).find('td').wrapInner('<div style="display: none;" />')
  .parent()
  .find('td > div')
  .slideDown(700, function(){
    var $set = $(this);
    $set.replaceWith($set.contents());
  });
	var interval = window.setTimeout(
		function() { newRow.removeClass('flash-new'); },
		self.insertFlashTime
	);
}

OrderBook.prototype.insertRowAfter = function(row, order) {
	var self = this;
	var htmlRow = this.generateRow(order);
	var newRow = $(htmlRow).insertAfter(row);
	$(newRow).find('td').wrapInner('<div style="display: none;" />')
  .parent()
  .find('td > div')
  .slideDown(700, function(){
		var $set = $(this);
		$set.replaceWith($set.contents());
	});
	var interval = window.setTimeout(
		function() { newRow.removeClass('flash-new'); },
		self.insertFlashTime
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
		'#offer-rows tr',
		function(orderA, orderB) { return (orderA >= orderB); },
		1200,
		800
	);
	window.setInterval(function() { offerTable.refresh(offerTable); }, 4000);
	
	var bidTable = new OrderBook(
		'/api/v1/orders/bids/',
		client.symbol.symbol_id,
		client.symbol,
		client.bidList,
		'#bid-rows tr',
		function(orderA, orderB) { return (orderA <= orderB); },
		1200,
		800
	);
	window.setInterval(function() { bidTable.refresh(bidTable); }, 4000);
});
