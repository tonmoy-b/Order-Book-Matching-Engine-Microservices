'use client';
import React, { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { usePathname } from 'next/navigation';
import { useSearchParams } from 'next/navigation';
import Link from 'next/link';


type Transaction = {
    mainClientID: string;
    counterPartyID: string;
    counterPartyOrderId: string;
    mainClientOrderType: string;
    mainClientTransactionAmount: string;
    spreadAmount: string;
    mainClientOrderId: string;
    transactionID: string;
    transactionVolume: string;
};

export default function TablePage() {
    const router = usePathname();
    const searchParams = useSearchParams()
    var id = searchParams?.get('query')
    const [transactions, setTransactions] = useState<Transaction[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [searchQuery, setSearchQuery] = useState('');

    useEffect(() => {
        fetch(`http://localhost:4001/transactions/clientId/${id}`)
            .then((res) => {
                if (!res.ok) throw new Error('Failed to fetch Transactions');
                return res.json();
            })
            .then((data: Transaction[]) => setTransactions(data))
            .catch((err) => setError(err.message))
            .finally(() => setLoading(false));
    }, []);

    // Filter transactions based on search query (case-insensitive)
    const filteredTransactions = transactions.filter((transaction) =>
        `${transaction.transactionID} ${transaction.mainClientID}`.toLowerCase().includes(searchQuery.toLowerCase())
    );


    return (
        <div className="min-h-screen bg-gray-100 p-6">
            <h1 className="text-3xl font-bold mb-6">Transaction List</h1>

            <input
                type="text"
                placeholder="Search by Transaction-ID or Party..."
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
                                <th className="py-3 px-6 text-left">transaction ID</th>
                                <th className="py-3 px-6 text-left">Counter Party ID</th>
                                <th className="py-3 px-6 text-left">Transaction Amount</th>
                                <th className="py-3 px-6 text-left">Transaction Volume</th>
                            </tr>
                        </thead>
                        <tbody className="text-gray-700 text-sm font-light">
                            {filteredTransactions.map((transaction) => (
                                <tr key={transaction.transactionID} className="border-b border-gray-200 hover:bg-gray-100">
                                    <td className="py-3 px-6">{transaction.transactionID}</td>
                                    <td className="py-3 px-6">{transaction.counterPartyID}</td>
                                    <td className="py-3 px-6">{transaction.mainClientTransactionAmount}</td>
                                    <td className="py-3 px-6">{transaction.transactionVolume}</td>
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
                        pathname: `/pending`,
                        query: { query: `${id}` },
                    }}
                >
                    Pending Orders
                </Link>
            </div>
        </div>

    );
}





