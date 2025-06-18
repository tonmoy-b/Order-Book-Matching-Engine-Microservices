'use client';
import React, { useState, useEffect } from "react";
import { useRouter } from 'next/navigation';
import { FormEvent } from "react";
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from "@/components/ui/card"


export default function Form02() {
  const router = useRouter(); // // 
  const [inputValue, setInputValue] = useState('');

  const handleSubmit = async (formDataEvent: FormEvent<HTMLFormElement>) => {
    formDataEvent.preventDefault();
    const formData = new FormData(formDataEvent.currentTarget);
    console.log(formData);
    const formObject = Object.fromEntries(formData.entries());
    const jsonFormPayload = JSON.stringify(formObject);
    const jsonBodyPayload = JSON.stringify({
      orderId: null,
      clientId: formObject.clientId,
      asset: formObject.asset,
      orderTime: "2025-05-17 00:20:00.000",
      orderType: formObject.orderType,
      amount: formObject.amount,
      volume: formObject.volume,

    });
    console.log(jsonBodyPayload);

    try {
      const response = await fetch('http://localhost:4000/orders', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: jsonBodyPayload,//JSON.stringify(),
      });

      if (response.ok) {
        const data = await response.json();
        // Handle success (e.g., show a message, reset the form)
        console.log('Success:', data);
        //
      } else {
        // Handle error
        console.error('Error---:', response.statusText);
      }

      console.log('llll: ', formData.get('clientId'));
      var aa = formData.get('clientId')

      router.push(`/table/?query=${inputValue}`);


    } catch (error) {
      console.error('Fetch error:', error);
    }

  };

  return (
    <Card className="w-full max-w-lg mx-auto">
      <CardHeader>
        <CardTitle>Order Information</CardTitle>
        <CardDescription>Please enter your Order Details below.</CardDescription>
      </CardHeader>
      <CardContent>

        <form className="max-w-md mx-auto" id="orderform"
          onSubmit={handleSubmit}>

          {/* <div className="relative z-0 w-full mb-5 group">
                        <input 
                        name="asset" 
                        id="asset" 
                        className="block py-2.5 px-0 w-full text-sm text-gray-900 bg-transparent border-0 border-b-2 border-gray-300 appearance-none dark:text-white dark:border-gray-600 dark:focus:border-blue-500 focus:outline-none focus:ring-0 focus:border-blue-600 peer" placeholder=" " required />
                        <label htmlFor="floating_email" 
                        className="peer-focus:font-medium absolute text-sm text-gray-500 dark:text-gray-400 duration-300 transform -translate-y-6 scale-75 top-3 -z-10 origin-[0] peer-focus:start-0 rtl:peer-focus:translate-x-1/4 rtl:peer-focus:left-auto peer-focus:text-blue-600 peer-focus:dark:text-blue-500 peer-placeholder-shown:scale-100 peer-placeholder-shown:translate-y-0 peer-focus:scale-75 peer-focus:-translate-y-6">Asset Ticker</label>
                        </div> */}

          <div className="relative z-0 w-full mb-5 group">
            <input
              name="asset"
              id="asset"
              className="block py-2.5 px-0 w-full text-sm text-gray-900 bg-transparent border-0 border-b-2 border-gray-300 appearance-none dark:text-white dark:border-gray-600 dark:focus:border-blue-500 focus:outline-none focus:ring-0 focus:border-blue-600 peer" placeholder=" " required />
            <label htmlFor="asset"
              className="peer-focus:font-medium absolute text-sm text-gray-500 dark:text-gray-400 duration-300 transform -translate-y-6 scale-75 top-3 -z-10 origin-[0] peer-focus:start-0 rtl:peer-focus:translate-x-1/4 rtl:peer-focus:left-auto peer-focus:text-blue-600 peer-focus:dark:text-blue-500 peer-placeholder-shown:scale-100 peer-placeholder-shown:translate-y-0 peer-focus:scale-75 peer-focus:-translate-y-6">Asset Ticker</label>
          </div>

          <div className="relative z-0 w-full mb-5 group">
            <input
              name="orderType"
              id="orderType"
              className="block py-2.5 px-0 w-full text-sm text-gray-900 bg-transparent border-0 border-b-2 border-gray-300 appearance-none dark:text-white dark:border-gray-600 dark:focus:border-blue-500 focus:outline-none focus:ring-0 focus:border-blue-600 peer" placeholder=" " required />
            <label htmlFor="orderType"
              className="peer-focus:font-medium absolute text-sm text-gray-500 dark:text-gray-400 duration-300 transform -translate-y-6 scale-75 top-3 -z-10 origin-[0] peer-focus:start-0 rtl:peer-focus:translate-x-1/4 rtl:peer-focus:left-auto peer-focus:text-blue-600 peer-focus:dark:text-blue-500 peer-placeholder-shown:scale-100 peer-placeholder-shown:translate-y-0 peer-focus:scale-75 peer-focus:-translate-y-6">
              Order Type (Bid / Ask) </label>
          </div>

          <div className="relative z-0 w-full mb-5 group">
            <input type="number" step={0.01}
              name="amount"
              id="amount"
              className="block py-2.5 px-0 w-full text-sm text-gray-900 bg-transparent border-0 border-b-2 border-gray-300 appearance-none dark:text-white dark:border-gray-600 dark:focus:border-blue-500 focus:outline-none focus:ring-0 focus:border-blue-600 peer" placeholder=" " required />
            <label htmlFor="amount"
              className="peer-focus:font-medium absolute text-sm text-gray-500 dark:text-gray-400 duration-300 transform -translate-y-6 scale-75 top-3 -z-10 origin-[0] peer-focus:start-0 rtl:peer-focus:translate-x-1/4 rtl:peer-focus:left-auto peer-focus:text-blue-600 peer-focus:dark:text-blue-500 peer-placeholder-shown:scale-100 peer-placeholder-shown:translate-y-0 peer-focus:scale-75 peer-focus:-translate-y-6">Amount
            </label>
          </div>

          <div className="relative z-0 w-full mb-5 group">
            <input type="number" step={1}
              name="volume"
              id="volume"
              className="block py-2.5 px-0 w-full text-sm text-gray-900 bg-transparent border-0 border-b-2 border-gray-300 appearance-none dark:text-white dark:border-gray-600 dark:focus:border-blue-500 focus:outline-none focus:ring-0 focus:border-blue-600 peer" placeholder=" " required />
            <label htmlFor="volume"
              className="peer-focus:font-medium absolute text-sm text-gray-500 dark:text-gray-400 duration-300 transform -translate-y-6 scale-75 top-3 -z-10 origin-[0] peer-focus:start-0 rtl:peer-focus:translate-x-1/4 rtl:peer-focus:left-auto peer-focus:text-blue-600 peer-focus:dark:text-blue-500 peer-placeholder-shown:scale-100 peer-placeholder-shown:translate-y-0 peer-focus:scale-75 peer-focus:-translate-y-6">
              Volume
            </label>
          </div>

          <div className="relative z-0 w-full mb-5 group">
            <input
              name="clientId"
              id="clientId"
              //
              value={inputValue}
              onChange={(e) => setInputValue(e.target.value)}

              //
              className="block py-2.5 px-0 w-full text-sm text-gray-900 bg-transparent border-0 border-b-2 border-gray-300 appearance-none dark:text-white dark:border-gray-600 dark:focus:border-blue-500 focus:outline-none focus:ring-0 focus:border-blue-600 peer" placeholder=" " required />
            <label htmlFor="clientId"
              className="peer-focus:font-medium absolute text-sm text-gray-500 dark:text-gray-400 duration-300 transform -translate-y-6 scale-75 top-3 -z-10 origin-[0] peer-focus:start-0 rtl:peer-focus:translate-x-1/4 rtl:peer-focus:left-auto peer-focus:text-blue-600 peer-focus:dark:text-blue-500 peer-placeholder-shown:scale-100 peer-placeholder-shown:translate-y-0 peer-focus:scale-75 peer-focus:-translate-y-6">Client Id</label>
          </div>
          <div className="relative z-0 w-full mb-5 group">
            <button type="submit" >Submit</button>
          </div>

        </form>
      </CardContent>
    </Card>

  );
}



