import pandas as pd
import requests
from requests import Session
import numpy
import json
import numpy as np
import math
from scipy.stats.stats import pearsonr

from flask import Flask,jsonify
from flask import request
app = Flask(__name__) #create the Flask app

@app.route('/wager_function')

def wager_function():

    '''Query and get risk, duration requested by user'''
    risk = request.args.get('risk') #if key doesn't exist, returns None
    if risk is None:
        risk = 20
    else:
        risk = (int)(request.args.get('risk'))
    duration =request.args.get('duration')
    if duration is None:
        duration = 50
    else:
        duration = (int)(request.args.get('duration'))
    

    '''
    Declare and initialize all the lists/arrays for manipulation
    Explicit initialization used to simplify readability
    '''
    number_of_records = 24
    topTokens = 5
    name = ["ETH"]*number_of_records*topTokens
    name_hodl = ["ETH"]*number_of_records*topTokens
    name_uniqueAddress = ["ETH"]*number_of_records*topTokens
    tokenName =["ETH"]*topTokens
    tokenRisk = [0,0,0,0,0]
    tokenName_lMC = ["ETH"]*topTokens
    tokenRisk_lMC = [0,0,0,0,0]
    tokenHodl_lMC = [0,0,0,0,0]
    tokenName_price = ["ETH"]*topTokens
    tokenRisk_price = [0,0,0,0,0]
    tokenHodl_price = [0,0,0,0,0]
    tokenName_stable = ["ETH"]*topTokens
    tokenRisk_stable = [0,0,0,0,0]
    tokenHODL_stable = [100,100,100,100,100] #stablecoin HODL index is out of range comparing with other tokens
    tokenName_utility = ["ETH"]*topTokens
    tokenRisk_utility = [0,0,0,0,0]
    tokenHODL_utility = ["ETH"]*topTokens
    token_uniqueAddressCount = [0,0,0,0,0]

    '''
    take daily snapshots of top 5 tokens based on liquid Market cap in descending order.
    Parse the json string, and use it to gather change in price, volatility and stock index
    Use the parameters to gain insight on tokens with good current liquid market cap
    '''

    '''
    This calculation estimates top tokens based on liquidMarketCap
    Based on token velocity an approximate HODL time is estimated by adapting the Buterin money supply formula with circulating supply
    Risk and HODL values can be used in the overall 
    '''
    url = "https://web3api.io/api/v2/tokens/rankings/historical"
    querystring = {"direction":"descending","sortType":"liquidMarketCap","topN":"5"}
    headers = {'x-api-key': 'UAK42f1c35cfd5527c86e5dc9df987af10f'}
    response = requests.request("GET", url, headers=headers, params=querystring)
    response_native = json.loads(response.text)
    response_data=response_native.get("payload").get("data")
    for j in range(0,5):
        for i in range(0,number_of_records):
            name[i+j*number_of_records] =  response_data[i].get("snapshot")[j].get("symbol")
            if(response_data[i].get("snapshot")[j].get("tokenVelocity") is None):
                name_hodl[i+j*number_of_records] = 0.00001
            else:
                name_hodl[i+j*number_of_records] = 1/(((float)(response_data[i].get("snapshot")[j].get("tokenVelocity")))*((float)(response_data[i].get("snapshot")[j].get("circulatingSupply"))))
            name_uniqueAddress[i+j*number_of_records] =  response_data[i].get("snapshot")[j].get("uniqueAddresses")
                
    unique,counts = np.unique(name,return_counts=True)
    j=0
    for i in range(0,len(counts)):
        '''80% of time token is top ranked'''
        '''Evaluate HODL time based on unique addresses involved in the transaction'''
        if(j<topTokens):
            if(counts[i]>number_of_records-7):
                tokenName_lMC[j] = unique[i]
                tokenRisk_lMC[j] = (int)(counts[i]*100/number_of_records)
                tokenHodl_lMC[j] = name_hodl[i]*100*name_uniqueAddress[i]
                j = j+1
    '''
    for j in range(0,topTokens):
        print(tokenName_lMC[j])
        print(tokenRisk_lMC[j])
        print(tokenHodl_lMC[j])
    '''
    '''Start update process of collecting the top tokens'''
    for i in range(0,topTokens):
        if(tokenHodl_lMC[i] < (100*duration)/60):
            tokenName[i] = tokenName_lMC[i]
            tokenRisk[i] = tokenRisk_lMC[i]
            print(tokenName[i])
            print(tokenRisk[i])
    '''
    This calculation estimates top tokens based on normalized change in price 
    The aim is to estimate high volatility coins
    Based on token velocity an approximate HODL time is estimated
    This is based on adapting the Buterin money supply formula with circulating supply
    This will tell us how long the coins can be kept since their value will go down rapidly
    '''
    if((duration <= 12)|(risk >70)):
        url = "https://web3api.io/api/v2/tokens/rankings/historical"
        querystring = {"direction":"descending","sortType":"changeInPrice","topN":"5"}
        headers = {'x-api-key': 'UAK42f1c35cfd5527c86e5dc9df987af10f'}
        response = requests.request("GET", url, headers=headers, params=querystring)
        response_native = json.loads(response.text)
        response_data=response_native.get("payload").get("data")
        value_price = [0]*number_of_records*topTokens
        value_hodl = [0]*number_of_records*topTokens
        for j in range(0,5):
            for i in range(0,number_of_records):
                name[i+j*number_of_records] =  response_data[i].get("snapshot")[j].get("symbol")
                value_hodl[i+j*number_of_records]
                value_price[i+j*number_of_records] = response_data[i].get("snapshot")[j].get("changeInPriceDaily")/response_data[i].get("snapshot")[j].get("currentPrice")
                if(response_data[i].get("snapshot")[j].get("tokenVelocity") is None):
                    value_hodl[i+j*number_of_records] = 0.00001
                else:
                    if(response_data[i].get("snapshot")[j].get("uniqueAddresses") is None):
                        value_hodl[i+j*number_of_records] = 1/(((float)(response_data[i].get("snapshot")[j].get("tokenVelocity")))*((float)(response_data[i].get("snapshot")[j].get("circulatingSupply"))))
                    else:
                        value_hodl[i+j*number_of_records] = (float)(response_data[i].get("snapshot")[j].get("uniqueAddresses"))/(((float)(response_data[i].get("snapshot")[j].get("tokenVelocity")))*((float)(response_data[i].get("snapshot")[j].get("circulatingSupply"))))
        unique,counts = np.unique(name,return_counts=True)
        j=0
        for i in range(0,len(counts)):
            if(j<topTokens):
                if((counts[i]>1)&(value_price[i]>100)):
                    tokenName_price[j] = unique[i]
                    tokenRisk_price[j] = value_price[i]/10
                    tokenHodl_price[j] = value_hodl[i]
                    if(tokenRisk_price[j]>100):
                        tokenRisk_price[j] = 100
                    j = j+1

        '''
        for j in range(0,topTokens):
            print(tokenName_price[j])
            print(tokenRisk_price[j])
            print(tokenHodl_price[j])
        '''
        for i in range(0,topTokens):
            tokenName[i] = tokenName_price[i]
            tokenRisk[i] = (float)(tokenRisk_price[i]*tokenHodl_price[i])/10000
            print(tokenName[i])
            print(tokenRisk[i])
    '''
    This uses the amber data API to estimate low token velocity and stable coins
    Gold tokens are expected to come over
    Token stability is estimated for HODL purposes
    Duration > 30 days and risk < 30% defaults here due to lack of data
    '''
    if((duration > 30) | (risk < 30)):
        url = "https://web3api.io/api/v2/tokens/rankings"
        querystring = {"direction":"descending","sortType":"tokenVelocity","timeInterval":"weeks"}
        headers = {'x-api-key': 'UAK42f1c35cfd5527c86e5dc9df987af10f'}

        response = requests.request("GET", url, headers=headers, params=querystring)
        response_native = json.loads(response.text)
        response_data=response_native.get("payload").get("data")
        for j in range(1,topTokens+1):
            tokenName_stable[j-1] = response_data[j].get("symbol")
            tokenRisk_stable[j-1] = (float)(response_data[j].get("liquidMarketCap"))/((float)(response_data[j].get("circulatingSupply"))*(float)(response_data[j].get("tokenVelocity"))*(float)(response_data[j].get("currentPrice")))
            if(tokenRisk_stable[j-1]>(float)(response_data[j].get("currentPrice"))):
                tokenRisk_stable[j-1]=100
            else:
                tokenRisk_stable[j-1]=100*tokenRisk_stable[j-1]/((float)(response_data[j].get("currentPrice")))
            print(tokenName_stable[j-1])
            print(tokenRisk_stable[j-1])

        for i in range(0,topTokens):
            tokenName[i] = tokenName_stable[i]
            tokenRisk[i] = tokenRisk_stable[i]
            print(tokenName[i])
            print(tokenRisk[i])
    
    '''
    This usection ses the amber data API to estimate transaction volume
    Utility tokens are expected to come over
    Token stability is estimated for HODL purposes
    '''
    if((duration > 12) & (duration < 30)):
        url = "https://web3api.io/api/v2/market/rankings"
        querystring = {"direction":"descending","sortType":"transactionVolume","timeInterval":"weeks"}
        headers = {'x-api-key': 'UAK42f1c35cfd5527c86e5dc9df987af10f'}

        response = requests.request("GET", url, headers=headers, params=querystring)
        response_native = json.loads(response.text)
        response_data=response_native.get("payload").get("data")
        for j in range(0,topTokens):
            tokenName_utility[j] = response_data[j].get("symbol")
            tokenRisk_utility[j] = 100*((float)(response_data[j].get("liquidMarketCap"))*((float)(response_data[j].get("changeInPriceWeekly")))/(float)(response_data[j].get("circulatingSupply")))
            print(tokenName_utility[j])
            if tokenRisk_utility[j] < 0: #stablecoin with high transaction volume --> low risk
                tokenRisk_utility[j] = 100
            print(tokenRisk_utility[j])
    
        for i in range(0,topTokens):
            tokenName[i] = tokenName_utility[i]
            tokenRisk[i] = tokenRisk_utility[i]
            print(tokenName[i])
            print(tokenRisk[i])
  

    for i in range(0,topTokens):
        if(tokenName[i] == "ETH"):
            tokenName[i] = tokenName_lMC[i]
            tokenRisk[i] = tokenRisk_lMC[i]
        if(tokenName[i] == "ETH"):
            tokenName[i] = "MKR"
            tokenRisk[i] = 50
        print(tokenName[i])
        print(tokenRisk[i])
    return jsonify({tokenName[0]:tokenRisk[0],tokenName[1]:tokenRisk[1],tokenName[2]:tokenRisk[2],tokenName[3]:tokenRisk[3],tokenName[4]:tokenRisk[4],"BTC":0})
if __name__ == "__main__":
    app.run(host="0.0.0.0", port=80) #run app in debug mode on port 5000