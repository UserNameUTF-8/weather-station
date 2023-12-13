from machine import Pin, UART, SoftI2C
from umqtt.robust import MQTTClient
import dht
import machine
import network
import sys
import time
from time import sleep
import BME280


net_ = network.WLAN(network.STA_IF)

net_.active(True)


i2c = SoftI2C(scl=Pin(22), sda=Pin(21), freq=10000)


if not net_.isconnected():
    print('connecting ...')
    connect('your-wifi-ssid', 'your-wifi-password')
    
    
broker_address = 'your-url-cloud-mqtt'
port_ = 8883  # Use 8883 for secure MQTT
client_id = 'client_'
username = 'cloud-username'
password_ = 'cloud-password'
ssl_param__ = {}



# you have to get cloud mqtt like mosquito, hivemq, and generate the pem certification
with open('server1.pem', 'wb') as file:
    cert__ = file.read()


ssl_param__['cert'] = cert__
ssl_param__['server_hostname'] = broker_address










client__ = MQTTClient(client_id,
                      server = broker_address,
                      port=port_,
                      user=username,
                      password=password_,
                      ssl=True,
                      ssl_params = ssl_param__
     )


client__.connect()



dht11_ = dht.DHT11(machine.Pin(26))
dht22_ = dht.DHT22(machine.Pin(27))
bme = BME280.BME280(i2c=i2c)


while True:
    
        pres = bme.pressure
        temp_bme = bme.temperature
        dht11_.measure()
        temp_11 = dht11_.temperature()
        hum_11 = dht11_.humidity()
        print('dht 11'.center(100))
        print(temp_11)
        print(hum_11)
        print('-'*50)
        dht22_.measure()
        print('dht 22'.center(100))
        temp_22 = dht22_.temperature()
        hum_22 = dht22_.humidity()
        print(temp_22)
        print(hum_22)
        print('bme'.center(100))
        print(temp_bme)
        print(pres)
    
        client__.publish(b'/info/temp', f"{temp_11}_T_{temp_22}_T_{temp_bme}")
        client__.publish(b'/info/hum', f'{hum_11}_T_{hum_22}')
        client__.publish(b'/info/pre', f'{pres}')
        client__.publish(b'/hum/dht11', str(hum_11))
        client__.publish(b'/hum/dht22', str(hum_22))
        
        
        print('ok')
        sleep(2)
         
            

        
            
    
        
        
            
            

            
            
          
        
        
    
    



