// CODE GENERATED BY JAVADUDE BEAN ANNOTATION PROCESSOR 
// -- DO NOT EDIT  -  THIS CODE WILL BE REGENERATED! --
package sample;

@javax.annotation.Generated(
    value = "com.javadude.annotation.processors.BeanAnnotationProcessor", 
    date = "Sun May 10 13:01:16 EDT 2009", 
    comments = "CODE GENERATED BY JAVADUDE BEAN ANNOTATION PROCESSOR; DO NOT EDIT! THIS CODE WILL BE REGENERATED!")
public abstract class TravelAgentGen  {
    private sample.IHotelAgent hotelAgent_;
    private sample.ICarAgent carAgent_;
    private sample.IFlightAgent flightAgent_;
    public TravelAgentGen() {
        ;
        hotelAgent_ = new sample.HotelAgentImpl();
        carAgent_ = new sample.CarAgentImpl();
        flightAgent_ = new sample.FlightAgentImpl();
    }


    public java.util.List<sample.IHotel> getHotels() {
        return hotelAgent_.getHotels();
    }
    public void reserve(sample.IHotel hotel) {
        hotelAgent_.reserve(hotel);
    }
    public java.util.List<sample.ICar> getCars() {
        return carAgent_.getCars();
    }
    public void reserve(sample.ICar car) {
        carAgent_.reserve(car);
    }
    public java.util.List<sample.IFlight> getFlight() {
        return flightAgent_.getFlight();
    }
    public void reserve(sample.IFlight flight) {
        flightAgent_.reserve(flight);
    }
    @Override
    public java.lang.String toString() {
        return getClass().getName() + '[' + paramString() + ']';
    }
    protected java.lang.String paramString() {
        return "";
    }
}