'use client';
import React, { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { usePathname } from 'next/navigation';
import { useSearchParams } from 'next/navigation';
import Link from 'next/link';

type Order = {
    orderId: string;
    clientId: string;
    asset: string;
    orderTime: string;
    orderType: string;
    amount: string;
    volume: string;
};

export default function TablePage() {
    const router = usePathname();
    const searchParams = useSearchParams()
    var id = searchParams?.get('query')
    const [orders, setOrders] = useState<Order[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [searchQuery, setSearchQuery] = useState('');

    useEffect(() => {
        fetch(`http://localhost:4001/transactions/pending/clientId/${id}`)
            .then((res) => {
                if (!res.ok) throw new Error('Failed To Fetch Pending Orders');
                return res.json();
            })
            .then((data: Order[]) => setOrders(data))
            .catch((err) => setError(err.message))
            .finally(() => setLoading(false));
    }, []);
    
     // Filter transactions based on search query (case-insensitive)
    const filteredOrders = orders.filter((order) =>
        `${order.asset} ${order.orderType}`.toLowerCase().includes(searchQuery.toLowerCase())
    );

     return (
        <div className="min-h-screen bg-gray-100 p-6">
            <h1 className="text-3xl font-bold mb-6">Pending Orders List</h1>

            <input
                type="text"
                placeholder="Search by Asset or Order Type..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="mb-6 px-4 py-2 w-full max-w-md border rounded-lg shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />

            {loading ? (
                <p className="text-gray-700">Loading...</p>
            ) : error ? (
                <p className="text-red-600">Error: {error}</p>
            ) : (
                <div className="overflow-x-auto">
                    <table className="min-w-full bg-white shadow rounded-lg">
                        <thead>
                            <tr className="bg-gray-200 text-gray-600 uppercase text-sm leading-normal">
                                <th className="py-3 px-6 text-left">Order ID</th>
                                <th className="py-3 px-6 text-left">Asset</th>
                                <th className="py-3 px-6 text-left">Bid / Ask</th>
                                <th className="py-3 px-6 text-left">Amount</th>
                                <th className="py-3 px-6 text-left">Volume</th>
                                <th className="py-3 px-6 text-left">Order Time</th>
                            </tr>
                        </thead>
                        <tbody className="text-gray-700 text-sm font-light">
                            {filteredOrders.map((order) => (
                                <tr key={order.orderId} className="border-b border-gray-200 hover:bg-gray-100">
                                    <td className="py-3 px-6">{order.orderId}</td>
                                    <td className="py-3 px-6">{order.asset}</td>
                                    <td className="py-3 px-6">{order.orderType}</td>
                                    <td className="py-3 px-6">{order.amount}</td>
                                    <td className="py-3 px-6">{order.volume}</td>
                                    <td className="py-3 px-6">{order.orderTime}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}
            <div className="fixed bottom-0 left-0 w-full bg-white border-t shadow-md p-4 flex justify-center space-x-8 z-50">
                <Link className="text-blue-500 hover:underline shadow-xl"
                    href={{
                        pathname: `/`,
                        query: { id: `${id}` },
                    }}
                >
                    Order Form
                </Link>
                <Link className="text-blue-500 hover:underline shadow-xl"
                    href={{
                        pathname: `/table`,
                        query: { query: `${id}` },
                    }}
                >
                    Transactions Table
                </Link>
            </div>
        </div>

    );


}