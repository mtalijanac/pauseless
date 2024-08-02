# InternTrie

The InternTrie structure allows for efficient storage and retrieval of objects 
using serialized data as keys. This is useful when working with low cardinality
data in large datasets. InternTrie usage is inspired by String intern method.


## Usecase

Common situation in software development is deserialization of large amount
of objects where actual cardinality of data is low. Imagine payment transactions. 
Transaction often have 'type' field and this field is going to have 3-5 different
possible values: 'authorization', 'atm', 'pos', 'error', 'query' etc. 

We can exploit this low cardinality for performance benefit. 

Common issue is with deserialization is that while only few values are used, number of 
deserializated Strings for status field will be equal to number of transactions.

String#intern method is designed for this situation. It is used used to lower memory 
usage. String #intern() method will return 'canonical representation' of a string. 
Or in plain Java: s.intern() == t.intern() for strings 's' and 't' where s.equals(t)
is true.

For example imagine loop which is unmarshalling the same transaction millions of times.
Over and over byte array is unmarshalled to a new transaction instance.
And transaction type is set to 'atm'. Those three bytes of value will
eat ~ 50 Mb of data for each million of transactions being unmarshalled.

However adding one additional line in unmarshaller will remove all that memory overhead:

    transaction.type = transaction.type.intern();


Using intern will free memory. However to use #intern() we **need** the reference 
to **the string** which will we intern. Thus intern **doesn't lower GC 
cost of code** as we need still need to unmarshall bytes to a string.
The one which will be freed very next moment, as they are replaced by 
canonical representation returned by #intern().

In order to avoid memory and GC costs we need to intern strings *before* they are allocated.
And this is situation where InternTrie is useful. InternTrie will take serialized 
data representation and intern deserialized objects based on that. 

InternTrie also works with any object type.


## Usage

Example of deserializing two distinct byte arrays of same value to a 
same string:

    // two distinct byte arrays, but of same content:
    byte[] sBytes = "Usage example".getBytes(UTF_8);
    byte[] tBytes = "Usage example".getBytes(UTF_8);
    
    InternTrie<String> it = new InternTrie<String>();
    
    String s = it.intern( sBytes, (objData) -> new String(objData, UTF_8) );
    String t = it.intern( tBytes, (objData) -> new String(objData, UTF_8) );

    assertEquals("Usage example", s);    // true
    assertTrue(s == t);                  // true



