// SPDX-License-Identifier: UNLICENSED
pragma solidity >=0.7.0 <0.9.0;

interface IOrderBooks {
    event OwnershipTransferred( address indexed previousOwner,address indexed newOwner ) ;
    function VERSION(  ) external view returns (bytes32 ) ;
    function addOrder( bytes32 _orderBookID,bytes32 _orderUid,uint256 _price ) external   ;
    function cancelOrder( bytes32 _orderBookID,bytes32 _orderUid,uint256 _price ) external   ;
    function exists( bytes32 _orderBookID,uint256 price ) external view returns (bool _exists) ;
    function first( bytes32 _orderBookID ) external view returns (uint256 _price) ;
    function getBookSize( bytes32 _orderBookID ) external view returns (uint256 ) ;
    function getHead( bytes32 _orderBookID,uint256 price ) external view returns (bytes32 head) ;
    function getNOrders( bytes32 _orderBookID,uint256 nPrice,uint256 nOrder,uint256 lastPrice,bytes32 lastOrder,uint256 _type ) external view returns (uint256[] memory prices, uint256[] memory quantities, uint256 , bytes32 ) ;
    function getNOrdersOld( bytes32 _orderBookID,uint256 n,uint256 _type ) external view returns (uint256[] memory , uint256[] memory ) ;
    function getNode( bytes32 _orderBookID,uint256 _price ) external view returns (uint256 price, uint256 parent, uint256 left, uint256 right, bool red, bytes32 head, uint256 size) ;
    function getQuantitiesAtPrice( bytes32 _orderBookID,uint256 _price ) external view returns (uint256[] memory ) ;
    function initialize(  ) external   ;
    function last( bytes32 _orderBookID ) external view returns (uint256 _price) ;
    function matchTrade( bytes32 _orderBookID,uint256 price,uint256 takerOrderRemainingQuantity,uint256 makerOrderRemainingQuantity ) external  returns (uint256 ) ;
    function next( bytes32 _orderBookID,uint256 price ) external view returns (uint256 _price) ;
    function nextOrder( bytes32 _orderBookID,uint256 _price,bytes32 _orderId ) external view returns (bytes32 nextId) ;
    function nextPrice( bytes32 _orderBookID,uint8 _side,uint256 _price ) external view returns (uint256 price) ;
    function orderListExists( bytes32 _orderBookID,uint256 _price ) external view returns (bool ) ;
    function owner(  ) external view returns (address ) ;
    function prev( bytes32 _orderBookID,uint256 price ) external view returns (uint256 _price) ;
    function removeFirstOrder( bytes32 _orderBookID,uint256 _price ) external   ;
    function renounceOwnership(  ) external   ;
    function root( bytes32 _orderBookID ) external view returns (uint256 _price) ;
    function transferOwnership( address newOwner ) external   ;
}