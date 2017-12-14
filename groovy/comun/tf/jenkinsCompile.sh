PATH=/usr/bin:/etc:/usr/sbin:/usr/ucb:$HOME/bin:/usr/bin/X11:/sbin:.
set -o vi
TERM=vt220;export TERM
ORACLE_SID=ora1; export ORACLE_SID
ORACLE_HOME=/home/ora817/app/oracle/product/8.1.7; export ORACLE_HOME
LD_LIBRARY_PATH=/home/ora817/app/oracle/product/8.1.7/lib; export LD_LIBRARY_PATH
LIBPATH=/usr/lib:/home/ora817/app/oracle/product/8.1.7/lib:/usr/lpp/cobolmf/coblib; export LIBPATH
TNS_ADMIN=$ORACLE_HOME/network/admin; export TNS_ADMIN
PATH=.:$ORACLE_HOME/bin:/usr/lbin:/home/dba/scripts:/usr/vac/bin:/eci/spool/:$PATH; export PATH
ORACLE_TERM=vt220;export ORACLE_TERM
EPC_DISABLED=TRUE; export EPC_DISABLED
NLS_LANG=AMERICAN_AMERICA.WE8DEC; export NLS_LANG
export PATH

if [ -s "$MAIL" ]           # This is at Shell startup.  In normal
then echo "$MAILMSG"        # operation, the Shell checks
fi                          # periodically.

unset  EXTSHM
umask  000
ORACLE_HOME=/home/ora817/app/oracle/product/8.1.7;export ORACLE_HOME
#ORACLE_SID=ora8;export ORACLE_SID
#ORACLE_SID=ora1;export ORACLE_SID
#ORACLE_SID=ora2;export ORACLE_SID
# CUIDADO: El SID ora1 solo es para desarrollo, no debe ir nunca a producción.
ORACLE_SID=ora1;export ORACLE_SID
TNS_ADMIN=$ORACLE_HOME/network/admin; export TNS_ADMIN
PATH=.:$ORACLE_HOME/bin:/usr/lbin:/home/dba/scripts:/usr/vac/bin:$PATH; export PATH
ORACLE_TERM=vt100;export ORACLE_TERM

rm -r /cajasac/trabajo/real/Ejecutables
mkdir /cajasac/trabajo/real/Ejecutables

echo "--------------------------------------------------------------------"
echo "Compiling BOMBEO"
echo "--------------------------------------------------------------------"
cd /cajasac/trabajo/real/BOMBEO
make
mkdir /cajasac/trabajo/real/Ejecutables/BOMBEO

cp bombeo/ttf10102 /cajasac/trabajo/real/Ejecutables/BOMBEO
cp bombeoOTC/ttf10104 /cajasac/trabajo/real/Ejecutables/BOMBEO
cp pararBombeo/ttf10106 /cajasac/trabajo/real/Ejecutables/BOMBEO
cp pararOTC/ttf10108 /cajasac/trabajo/real/Ejecutables/BOMBEO

cd /cajasac/trabajo/real/Ejecutables/BOMBEO
tar cvf /cajasac/trabajo/real/Ejecutables/BOMBEO_RESULT.tar *
cd ..
rm -r /cajasac/trabajo/real/Ejecutables/BOMBEO

echo "--------------------------------------------------------------------"
echo "Compiling FINDIA"
echo "--------------------------------------------------------------------"
cd /cajasac/trabajo/real/FINDIA
make
mkdir /cajasac/trabajo/real/Ejecutables/FINDIA

cp actfindia/ttf00136 /cajasac/trabajo/real/Ejecutables/FINDIA
cp clientoc/ttf00108 /cajasac/trabajo/real/Ejecutables/FINDIA
cp concaroc/ttf00130 /cajasac/trabajo/real/Ejecutables/FINDIA
cp detalist/ttf00114 /cajasac/trabajo/real/Ejecutables/FINDIA
cp detaotc/ttf00118 /cajasac/trabajo/real/Ejecutables/FINDIA
cp detaseso/ttf00122 /cajasac/trabajo/real/Ejecutables/FINDIA
cp detavp/ttf00138 /cajasac/trabajo/real/Ejecutables/FINDIA
cp docfact/ttf00116 /cajasac/trabajo/real/Ejecutables/FINDIA
cp envdiari/bin/ttf00141 /cajasac/trabajo/real/Ejecutables/FINDIA
cp genficde/ttf00134 /cajasac/trabajo/real/Ejecutables/FINDIA
cp genfindia/ttf00102 /cajasac/trabajo/real/Ejecutables/FINDIA
cp genrollo/bin/stf00140 /cajasac/trabajo/real/Ejecutables/FINDIA
cp genrollo/bin/ttf00140 /cajasac/trabajo/real/Ejecutables/FINDIA
cp hacebas/ttf00104 /cajasac/trabajo/real/Ejecutables/FINDIA
cp linoberr/ttf00126 /cajasac/trabajo/real/Ejecutables/FINDIA
cp lotcerr/ttf00124 /cajasac/trabajo/real/Ejecutables/FINDIA
cp lstdia/ttf00132 /cajasac/trabajo/real/Ejecutables/FINDIA
cp provi/ttf00106 /cajasac/trabajo/real/Ejecutables/FINDIA
cp tccsalis/ttf00110 /cajasac/trabajo/real/Ejecutables/FINDIA
cp tctlist/ttf00112 /cajasac/trabajo/real/Ejecutables/FINDIA
cp totceleb/ttf00120 /cajasac/trabajo/real/Ejecutables/FINDIA
cp totcotc/tctotc /cajasac/trabajo/real/Ejecutables/FINDIA
cp xcom1/ttf00128 /cajasac/trabajo/real/Ejecutables/FINDIA

cp /cajasac/trabajo/real/FINDIA/envdiari/etc/ttf00141.cfg /cajasac/trabajo/real/Ejecutables/FINDIA

cd /cajasac/trabajo/real/Ejecutables/FINDIA
tar cvf /cajasac/trabajo/real/Ejecutables/FINDIA_RESULT.tar *
cd ..
rm -r /cajasac/trabajo/real/Ejecutables/FINDIA

echo "--------------------------------------------------------------------"
echo "Compiling AperturaCentro"
echo "--------------------------------------------------------------------"
cd /cajasac/trabajo/real/AperturaCentro/ttf01000
make
mkdir /cajasac/trabajo/real/Ejecutables/AperturaCentro

cp /cajasac/trabajo/real/AperturaCentro/ttf01000/bin/ttf01000 /cajasac/trabajo/real/Ejecutables/AperturaCentro

cd /cajasac/trabajo/real/AperturaCentro/ttf01001
make
cp /cajasac/trabajo/real/AperturaCentro/ttf01001/bin/ttf01001 /cajasac/trabajo/real/Ejecutables/AperturaCentro

cd /cajasac/trabajo/real/AperturaCentro/ttf01100
make
cp /cajasac/trabajo/real/AperturaCentro/ttf01100/bin/ttf01100 /cajasac/trabajo/real/Ejecutables/AperturaCentro

cp /cajasac/trabajo/real/AperturaCentro/ttf01001/etc/APERTURA.CFG /cajasac/trabajo/real/Ejecutables/AperturaCentro
cp /cajasac/trabajo/real/AperturaCentro/ttf01001/etc/BORRADOS.CFG /cajasac/trabajo/real/Ejecutables/AperturaCentro

cd /cajasac/trabajo/real/Ejecutables/AperturaCentro
tar cvf /cajasac/trabajo/real/Ejecutables/AperturaCentro_RESULT.tar *
cd ..
rm -r /cajasac/trabajo/real/Ejecutables/AperturaCentro

echo "--------------------------------------------------------------------"
echo "Compiling Inesperados"
echo "--------------------------------------------------------------------"
cd /cajasac/trabajo/real/Inesperados
make
mkdir /cajasac/trabajo/real/Ejecutables/Inesperados

cp cotizaciones /cajasac/trabajo/real/Ejecutables/Inesperados
cp inesperados /cajasac/trabajo/real/Ejecutables/Inesperados

cd /cajasac/trabajo/real/Ejecutables/Inesperados
tar cvf /cajasac/trabajo/real/Ejecutables/Inesperados_RESULT.tar *
cd ..
rm -r /cajasac/trabajo/real/Ejecutables/Inesperados

#echo "--------------------------------------------------------------------"
#echo "Generando TAR"
#echo "--------------------------------------------------------------------"
#cd /cajasac/trabajo/real/Ejecutables
#tar cvf TerminalFinanciero.tar *

echo "--------------------------------------------------------------------"
echo "Enviando TARs a FTP corporativo"
echo "--------------------------------------------------------------------"
ftp -in 192.168.74.79<<FIN_FTP
user jenkins 12jenki8
binary
lcd "/cajasac/trabajo/real/Ejecutables"
cd /dejar
mkdir TerminalFinanciero
cd TerminalFinanciero
put BOMBEO_RESULT.tar
put FINDIA_RESULT.tar
put AperturaCentro_RESULT.tar
put Inesperados_RESULT.tar

bye
FIN_FTP
