!C++

#class
<_
class CLASS_NAME
{
   ;
};
_>

#subclass
<_
class CLASS_NAME : public PARENT_CLASS
{
   ;
};
_>

#method
<_
RETURN_TYPE CLASS::METHOD (TYPE1 PARAM1, TYPE2 PARAM2)
{
   ;
}
_>

#function
<_
RETURN_TYPE FUNCTION (TYPE1 PARAM1, TYPE2 PARAM2)
{
   ;
}
_>

#compound statement
<_
{
   ;
}
_>

#if / then
<_
if (EXPRESSION)
{
   ;
}
_>

#if / else
<_
if (EXPRESSION) 
{
   ;
}
else
{
   ;
}
_>

#for loop
<_
for (INITALIZATION; TEST_EXPRESSION; INCREMENT)
{
   ;
}
_>

#do loop
<_
do
{
   ;
}
while (EXPRESSION);
_>

#while loop
<_
while (EXPRESSION)
{
   ;
}
_>

#switch
<_
switch (EXPRESSION)
{
   case 1:
      ;
   case 2:
      ;
}
_>

#template class
<_
template<class CLASS_PARAM> class CLASS_NAME
{
   ;
};
_>

#template method
<_
template<class CLASS_PARAM> RETURN_TYPE CLASS<CLASS_PARAM>::METHOD (TYPE1 PARAM1, TYPE2 PARAM2)
{
   ;
}
_>

