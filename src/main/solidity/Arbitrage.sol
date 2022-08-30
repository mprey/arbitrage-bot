// SPDX-License-Identifier: UNLICENSED
pragma solidity >=0.7.0 <0.9.0;

interface Arbitrage {
    event OwnershipTransferred( address indexed previousOwner,address indexed newOwner ) ;
    function ADDRESSES_PROVIDER(  ) external view returns (address ) ;
    function POOL(  ) external view returns (address ) ;
    function approveTokens( address[] memory tokens ) external   ;
    function execute( uint256 amount,address start,bytes memory params ) external   ;
    function executeOperation( address asset,uint256 amount,uint256 premium,address ,bytes memory params ) external  returns (bool ) ;
    function owner(  ) external view returns (address ) ;
    function renounceOwnership(  ) external   ;
    function transferOwnership( address newOwner ) external   ;
    function withdraw(  ) external   ;
    function withdrawToken( address token ) external  returns (bool ) ;
    receive () external payable;
}