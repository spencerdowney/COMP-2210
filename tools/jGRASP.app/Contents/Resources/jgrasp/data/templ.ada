!Ada

#block
<_
begin
   null;
end;
_>

#block with declarations
<_
declare
   VARIABLE : VARIABLE_TYPE;
begin
   null;
end;
_>

#case
<_
case CASE_EXPRESSION is
   when CHOICE =>
      null;
end case;
_>

#exception handler
<_
exception
   when AN_ERROR =>
      null;
_>

#function spec
<_
function FUNCTION_NAME (PARAM1 : TYPE1;
                        PARAM2 : TYPE2) return VARIABLE_TYPE;
_>

#function body
<_
function FUNCTION_NAME (PARAM1 : TYPE1;
                        PARAM2 : TYPE2) return VARIABLE_TYPE is

begin
   null;
end FUNCTION_NAME;
_>

#guarded select
<_
select
   when CONDITION_1 =>
      accept ENTRY_NAME_1 do
         null;
      end;
or
   when CONDITION_2 =>
      accept ENTRY_NAME_2 do
         null;
      end;
end select;
_>

#if / then
<_
if CONDITION then
   null;
end if;
_>

#if / else
<_
if CONDITION then
   null;
else
   null;
end if;
_>

#for loop
<_
for INDEX_VARIABLE in VALUE_RANGE loop
   null;
end loop;
_>

#loop with exit
<_
loop
   null;
   exit when CONDITION;
end loop;
_>

#while loop
<_
while CONDITION loop
   null;
end loop;
_>

#package spec
<_
package PACKAGE_NAME is

end PACKAGE_NAME;
_>

#package body
<_
package body PACKAGE_NAME is

begin
   null;
end PACKAGE_NAME;
_>

#procedure spec
<_
procedure PROCEDURE_NAME (PARAM1 : TYPE1;
                          PARAM2 : TYPE2);
_>

#procedure body
<_
procedure PROCEDURE_NAME (PARAM1 : TYPE1;
                          PARAM2 : TYPE2) is

begin
   null;
end PROCEDURE_NAME;
_>

#rendezvous
<_
accept C do
   null;
end;
_>

#select
<_
select
   accept ENTRY_NAME_1 do
      null;
   end;
or
   accept ENTRY_NAME_2 do
      null;
   end;
end select;
_>

#task spec
<_
task TASK_NAME;
_>

#task body
<_
task body TASK_NAME is
begin
   null;
end TASK_NAME;
_>


