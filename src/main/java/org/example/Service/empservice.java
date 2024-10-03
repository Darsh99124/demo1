package org.example.Service;

import org.example.Employee;
import org.example.Repository.emprepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@Service
public class empservice {

    @Autowired
    public emprepo emprepo1;
    public List<Employee> add(List<Employee> emp)
    {
        for(int i=0;i<emp.size();i++)
        {
            emprepo1.save(emp.get(i));
        }
        return emp;

    }
}
